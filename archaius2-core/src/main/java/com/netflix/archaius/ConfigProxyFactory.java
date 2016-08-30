package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

/**
 * Factory for binding a configuration interface to properties in a {@link PropertyFactory}
 * instance.  Getter methods on the interface are mapped by naming convention
 * by the property name may be overridden using the @PropertyName annotation.
 * 
 * For example,
 * <pre>
 * {@code 
 * {@literal @}Configuration(prefix="foo")
 * interface FooConfiguration {
 *    int getTimeout();     // maps to "foo.timeout"
 *    
 *    String getName();     // maps to "foo.name"
 * }
 * }
 * </pre>
 * 
 * To create a proxy instance,
 * <pre>
 * {@code 
 * FooConfiguration fooConfiguration = configProxyFactory.newProxy(FooConfiguration.class);
 * }
 * </pre>
 * 
 * To override the prefix in {@literal @}Configuration or provide a prefix when there is no 
 * @Configuration annotation simply pass in a prefix in the call to newProxy.
 * 
 * <pre>
 * {@code 
 * FooConfiguration fooConfiguration = configProxyFactory.newProxy(FooConfiguration.class, "otherprefix.foo");
 * }
 * </pre>
 * 
 * By default all properties are dynamic and can therefore change from call to call.  To make the
 * configuration static set the immutable attributes of @Configuration to true.
 * 
 * Note that an application should normally have just one instance of ConfigProxyFactory
 * and PropertyFactory since PropertyFactory caches {@link com.netflix.archaius.api.Property} objects.
 * 
 * @see {@literal }@Configuration
 */
public class ConfigProxyFactory {

    /**
     * The decoder is used for the purpose of decoding any @DefaultValue annotation
     */
    private final Decoder decoder;
    private final PropertyFactory propertyFactory;
    private final Config config;
    
    @Inject
    public ConfigProxyFactory(Config config, Decoder decoder, PropertyFactory factory) {
        this.decoder = decoder;
        this.config = config;
        this.propertyFactory = factory;
    }
    
    public ConfigProxyFactory(Config config, PropertyFactory factory) {
        this(config, DefaultDecoder.INSTANCE, factory);
    }
    
    public ConfigProxyFactory(Config config) {
        this(config, DefaultPropertyFactory.from(config));
    }
    
    /**
     * Create a proxy for the provided interface type for which all getter methods are bound
     * to a Property.
     * 
     * @param type
     * @param config
     * @return
     */
    public <T> T newProxy(final Class<T> type) {
        return newProxy(type, null);
    }
    
    private String derivePrefix(Configuration annot, String prefix) {
        if (prefix == null && annot != null) {
            prefix = annot.prefix();
            if (prefix == null) {
                prefix = "";
            }
        }
        if (prefix == null) 
            return "";
        
        if (prefix.endsWith(".") || prefix.isEmpty())
            return prefix;
        
        return prefix + ".";
    }
    
    public <T> T newProxy(final Class<T> type, final String initialPrefix) {
        Configuration annot = type.getAnnotation(Configuration.class);
        return newProxy(type, initialPrefix, annot == null ? false : annot.immutable());
    }
    
    /**
     * Encapsulated the invocation of a single method of the interface
     *
     * @param <T>
     */
    public static interface MethodInvoker<T> {
        /**
         * Invoke the method with the provided arguments
         * @param args
         * @return
         */
        T invoke(Object obj, Object[] args);

        /**
         * Return the property key
         * @return
         */
        String getKey();
    }
    
    /**
     * Abstract method invoker that encapsulates a property
     * @param <T>
     */
    private static abstract class PropertyMethodInvoker<T> extends AbstractProperty<T> implements MethodInvoker<T> {
        public PropertyMethodInvoker(String key) {
            super(key);
        }
        
        @Override
        public T invoke(Object Obj, Object[] args) {
            return get();
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    <T> T newProxy(final Class<T> type, final String initialPrefix, boolean immutable) {
        Configuration annot = type.getAnnotation(Configuration.class);
        
        final String prefix = derivePrefix(annot, initialPrefix);
        
        // Iterate through all declared methods of the class looking for setter methods.
        // Each setter will be mapped to a Property<T> for the property name:
        //      prefix + lowerCamelCaseDerivedPropertyName
        final Map<Method, MethodInvoker<?>> invokers = new HashMap<>();

        for (Method m : type.getMethods()) {
            try {
                final String verb;
                if (m.getName().startsWith("get")) {
                    verb = "get";
                } else if (m.getName().startsWith("is")) {
                    verb = "is";
                } else {
                    verb = "";
                }
                
                Object defaultValue = null;
                if (m.getAnnotation(DefaultValue.class) != null) {
                    String value = m.getAnnotation(DefaultValue.class).value();
                    if (m.getReturnType() == String.class) {
                        defaultValue = config.getString("*", value);
                    } else {
                        defaultValue = decoder.decode(m.getReturnType(), config.getString("*", value));
                    }
                } 
                
                final Class<?> returnType = m.getReturnType();
                final PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
                final String propName = nameAnnot != null && nameAnnot.name() != null
                                ? prefix + nameAnnot.name()
                                : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);
    
                // For sub-interfaces create a proxy instance where the same proxy instance is returned but its
                // methods can still return dynamic values
                if (returnType.equals(Map.class)) {
                    invokers.put(m, createMapProperty(propName, (ParameterizedType)m.getGenericReturnType(), immutable));
                } else if (returnType.isInterface()) {
                    invokers.put(m, createInterfaceProperty(propName, newProxy(returnType, propName, immutable)));
                } else if (m.getParameterTypes().length > 0) {
                    invokers.put(m, createParameterizedProperty(returnType, propName, nameAnnot.name(), defaultValue));
                } else if (immutable) {
                    invokers.put(m, createImmutablePropertyWithDefault(m.getReturnType(), propName, defaultValue));
                } else {
                    invokers.put(m, createDynamicProperty(m.getReturnType(), propName, defaultValue));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error proxying method " + m.getName(), e);
            }
        }
        
        final MethodHandles.Lookup temp;
        try {
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                    .getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            temp = constructor.newInstance(type, MethodHandles.Lookup.PRIVATE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary object for " + type.getName(), e);
        }
        
        final InvocationHandler handler = (proxy, method, args) -> {
            MethodInvoker<?> invoker = invokers.get(method);
            if (invoker != null) {
                Object result = invoker.invoke(proxy, args);
                if (result == null && method.isDefault()) {
                    result = temp.unreflectSpecial(method, type)
                            .bindTo(proxy)
                            .invokeWithArguments();
                }
                return result;
            }
            
            if ("toString".equals(method.getName())) {
                StringBuilder sb = new StringBuilder();
                sb.append(type.getSimpleName()).append("[");
                Iterator<Entry<Method, MethodInvoker<?>>> iter = invokers.entrySet().iterator();
                while (iter.hasNext()) {
                    MethodInvoker entry = iter.next().getValue();
                    sb.append(entry.getKey().substring(prefix.length())).append("='");
                    try {
                        sb.append(entry.invoke(proxy, null));
                    } catch (Exception e) {
                        sb.append(e.getMessage());
                    }
                    sb.append("'");
                    if (iter.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                return sb.toString();
            } else {
                throw new NoSuchMethodError(method.getName() + " not found on interface " + type.getName());
            }
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler);
    }
    
    @SuppressWarnings("unchecked")
    private <T> MethodInvoker<T> createMapProperty(final String propName, final ParameterizedType type, final boolean immutable) {
        final Class<?> valueType = (Class<?>)type.getActualTypeArguments()[1];
        Map<String, Object> map = new ReadOnlyMap<String, Object>() {
            Map<String, Object> lookup = new ConcurrentHashMap<String, Object>();
            @Override
            public Object get(final Object key) {
                return lookup.computeIfAbsent((String) key, new Function<String, Object>() {
                    @Override
                    public Object apply(String key) {
                        return newProxy(valueType, propName + "." + key, immutable);
                    }
                });
            }
        };
        
        return (MethodInvoker<T>) createInterfaceProperty(propName, map);
    }

    protected <T> Supplier<T> defaultValueFromString(Class<T> type, String defaultValue) {
        return () -> decoder.decode(type, defaultValue);
    }
    
    protected <T> MethodInvoker<T> createImmutablePropertyWithDefault(final Class<T> type, final String propName, final Object defaultValue) {
        return new PropertyMethodInvoker<T>(propName) {
            private volatile T cached;
            
            @Override
            public T invoke(Object obj, Object[] args) {
                if (cached == null) {
                    cached = get();
                }
                return cached;
            }
            
            @Override
            public T get() {
                return propertyFactory.getProperty(propName).asType(type, (T)defaultValue).get();
            }
        };
    }
    
    protected <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy) {
        return new PropertyMethodInvoker<T>(propName) {
            @Override
            public T get() {
                return proxy;
            }
        };
    }

    protected <T> MethodInvoker<T> createDynamicProperty(final Class<T> type, final String propName, final Object defaultValue) {
        final Property<T> prop = propertyFactory
                .getProperty(propName)
                .asType(type, (T)defaultValue);
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object obj, Object[] args) {
                return prop.get();
            }

            @Override
            public String getKey() {
                return prop.getKey();
            }
        };
    }
    
    protected <T> MethodInvoker<T> createParameterizedProperty(final Class<T> returnType, final String propName, final String nameAnnot, Object defaultValue) {
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object obj, Object[] args) {
                // Determine the actual property name by replacing with arguments using the argument index
                // to the method.  For example,
                //      @PropertyName(name="foo.${1}.${0}")
                //      String getFooValue(String arg0, Integer arg1) 
                // 
                // called as getFooValue("bar", 1) would look for the property 'foo.1.bar'
                Map<String, Object> values = new HashMap<>();
                for (int i = 0; i < args.length; i++) {
                    values.put("" + i, args[i]);
                }
                String propName = new StrSubstitutor(values, "${", "}", '$').replace(nameAnnot);
                return getPropertyWithDefault(returnType, propName, (T)defaultValue);
            }

            <R> R getPropertyWithDefault(Class<R> type, String propName, R defaultValue) {
                return propertyFactory.getProperty(propName).asType(type, defaultValue).get();
            }

            @Override
            public String getKey() {
                return propName;
            }
        }; 
    }
}
