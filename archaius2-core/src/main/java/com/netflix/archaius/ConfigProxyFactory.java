package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
 * Default values may be set by adding a {@literal @}DefaultValue with a default value string.  Note
 * that the default value type is a string to allow for interpolation.  Alternatively, methods can  
 * provide a default method implementation.  Note that {@literal @}DefaultValue cannot be added to a default
 * method as it would introduce ambiguity as to which mechanism wins.
 * 
 * For example,
 * <pre>
 * {@code
 * {@literal @}Configuration(prefix="foo")
 * interface FooConfiguration {
 *    @DefaultValue("1000")
 *    int getReadTimeout();     // maps to "foo.timeout"
 *    
 *    default int getWriteTimeout() {
 *        return 1000;
 *    }
 * }
 * }
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
    
    @Deprecated
    public ConfigProxyFactory(Config config, PropertyFactory factory) {
        this.decoder = config.getDecoder();
        this.config = config;
        this.propertyFactory = factory;
    }
    
    @Deprecated
    public ConfigProxyFactory(Config config) {
        this.decoder = config.getDecoder();
        this.config = config;
        this.propertyFactory = DefaultPropertyFactory.from(config);
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
                
                final Class<?> returnType = m.getReturnType();
                
                Object defaultValue = null;
                if (m.getAnnotation(DefaultValue.class) != null) {
                    if (m.isDefault()) {
                        throw new IllegalArgumentException("@DefaultValue cannot be defined on a method with a default implementation for method "
                                + m.getDeclaringClass().getName() + "#" + m.getName());
                    } else if (
                            Map.class.isAssignableFrom(returnType) ||
                            List.class.isAssignableFrom(returnType) ||
                            Set.class.isAssignableFrom(returnType) ) {
                        throw new IllegalArgumentException("@DefaultValue cannot be used with collections.  Use default method implemenation instead "
                                + m.getDeclaringClass().getName() + "#" + m.getName());
                    }
                    
                    String value = m.getAnnotation(DefaultValue.class).value();
                    if (returnType == String.class) {
                        defaultValue = config.getString("*", value);
                    } else {
                        defaultValue = decoder.decode(returnType, config.getString("*", value));
                    }
                } 
                
                final PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
                final String propName = nameAnnot != null && nameAnnot.name() != null
                                ? prefix + nameAnnot.name()
                                : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);
    
                // For sub-interfaces create a proxy instance where the same proxy instance is returned but its
                // methods can still return dynamic values
                if (returnType.equals(Map.class)) {
                    invokers.put(m, createMapProperty(propName, (ParameterizedType)m.getGenericReturnType(), immutable));
                } else if (returnType.equals(Set.class)) {
                    invokers.put(m, createSetProperty(propName, (ParameterizedType)m.getGenericReturnType(), LinkedHashSet::new));
                } else if (returnType.equals(SortedSet.class)) {
                    invokers.put(m, createSetProperty(propName, (ParameterizedType)m.getGenericReturnType(), TreeSet::new));
                } else if (returnType.equals(List.class)) {
                    invokers.put(m, createListProperty(propName, (ParameterizedType)m.getGenericReturnType(), ArrayList::new));
                } else if (returnType.equals(LinkedList.class)) {
                    invokers.put(m, createListProperty(propName, (ParameterizedType)m.getGenericReturnType(), LinkedList::new));
                } else if (returnType.isInterface()) {
                    invokers.put(m, createInterfaceProperty(propName, newProxy(returnType, propName, immutable)));
                } else if (m.getParameterTypes() != null && m.getParameterTypes().length > 0) {
                    if (nameAnnot == null) {
                        throw new IllegalArgumentException("Missing @PropertyName annotation on " + m.getDeclaringClass().getName() + "#" + m.getName());
                    }
                    invokers.put(m, createParameterizedProperty(returnType, propName, nameAnnot.name(), defaultValue));
                } else if (immutable) {
                    invokers.put(m, createImmutablePropertyWithDefault(returnType, propName, defaultValue));
                } else {
                    invokers.put(m, createScalarProperty(returnType, propName, defaultValue));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error proxying method " + m.getName(), e);
            }
        }
        
        // Hack so that default interface methods may be called from a proxy
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
    
    protected <T> MethodInvoker<T> createCustomProperty(final Function<String, T> converter, final String propName) {
        final Property<T> prop = propertyFactory
                .getProperty(propName)
                .asType(converter, null);
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
    
    private MethodInvoker<?> createListProperty(String propName, ParameterizedType type, Supplier<List> listSupplier) {
        final Class<?> valueType = (Class<?>)type.getActualTypeArguments()[0];
        return createCustomProperty(s -> { 
            List list = listSupplier.get();
            Arrays.asList(s.split("\\s*,\\s*")).forEach(v -> list.add(decoder.decode(valueType, v)));
            return list;
        }, propName);
    }

    private MethodInvoker<?> createSetProperty(String propName, ParameterizedType type, Supplier<Set> setSupplier) {
        final Class<?> valueType = (Class<?>)type.getActualTypeArguments()[0];
        return createCustomProperty(s -> { 
            Set set = setSupplier.get();
            Arrays.asList(s.split("\\s*,\\s*")).forEach(v -> set.add(decoder.decode(valueType, v)));
            return set;
        }, propName);
    }

    @SuppressWarnings("unchecked")
    private <T> MethodInvoker<T> createMapProperty(final String propName, final ParameterizedType type, final boolean immutable) {
        final Class<?> valueType = (Class<?>)type.getActualTypeArguments()[1];
        Map<String, Object> map;
        // This is a map for String -> Interface so create a proxy for any value
        if (valueType.isInterface()) {
            map = new ReadOnlyMap<String, Object>() {
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
        } else {
        // This is a map of String -> DecodableType (i.e. String, Long, etc...) 
            map = new ReadOnlyMap<String, Object>() {
                @Override
                public Object get(final Object key) {
                    return config.get(valueType, propName + "." + key);
                }
            };
        }
        
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

    protected <T> MethodInvoker<T> createScalarProperty(final Class<T> type, final String propName, final Object defaultValue) {
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
