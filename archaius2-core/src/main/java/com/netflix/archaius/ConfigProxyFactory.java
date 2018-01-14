package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyRepository;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

import org.apache.commons.lang3.text.StrSubstitutor;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.text.StrSubstitutor;

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
    private final PropertyRepository propertyRepository;
    private final Config config;
    
    @Inject
    public ConfigProxyFactory(Config config, Decoder decoder, PropertyFactory factory) {
        this.decoder = decoder;
        this.config = config;
        this.propertyRepository = factory;
    }
    
    @Deprecated
    public ConfigProxyFactory(Config config, PropertyFactory factory) {
        this.decoder = config.getDecoder();
        this.config = config;
        this.propertyRepository = factory;
    }
    
    @Deprecated
    public ConfigProxyFactory(Config config) {
        this.decoder = config.getDecoder();
        this.config = config;
        this.propertyRepository = DefaultPropertyFactory.from(config);
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
    static interface MethodInvoker<T> {
        /**
         * Invoke the method with the provided arguments
         * @param args
         * @return
         */
        T invoke(Object[] args);
    }

    static class DefaultMethodValueSupplier<T> implements Supplier<T> {
        private final MethodHandle handle;

        DefaultMethodValueSupplier(MethodHandle handle) {
            this.handle = handle;
        }

        @SuppressWarnings("unchecked")
		@Override
        public T get() {
            try {
                return (T) handle.invokeWithArguments();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Abstract method invoker that encapsulates a property
     * @param <T>
     */
    private static abstract class PropertyMethodInvoker<T> extends AbstractProperty<T> implements MethodInvoker<T> {
        private final Supplier<T> next;

        public PropertyMethodInvoker(String key, Supplier<T> next) {
            super(key);
            this.next = next;
        }
        
        @Override
        public T invoke(Object[] args) {
            T result = get();
            if (result == null) {
                return next.get();
            }
            return result;
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
        final Map<Method, String> propertyNames = new HashMap<>();

        final InvocationHandler handler = (proxy, method, args) -> {
            MethodInvoker<?> invoker = invokers.get(method);
            if (invoker != null) {
                return invoker.invoke(args);
            }

            if ("toString".equals(method.getName())) {
                StringBuilder sb = new StringBuilder();
                sb.append(type.getSimpleName()).append("[");
                sb.append(invokers.entrySet().stream().map(entry -> {
                	StringBuilder sbProperty = new StringBuilder();
                	sbProperty.append(propertyNames.get(entry.getKey()).substring(prefix.length())).append("='");
                    try {
                    	sbProperty.append(entry.getValue().invoke(null));
                    } catch (Exception e) {
                    	sbProperty.append(e.getMessage());
                    }
                    sbProperty.append("'");
                    return sbProperty.toString();
                }).collect(Collectors.joining(",")));
                sb.append("]");
                return sb.toString();
            } else {
                throw new NoSuchMethodError(method.getName() + " not found on interface " + type.getName());
            }
        };

        // Hack so that default interface methods may be called from a proxy
        final MethodHandles.Lookup lookup;
        try {
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                    .getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            lookup = constructor.newInstance(type, MethodHandles.Lookup.PRIVATE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary object for " + type.getName(), e);
        }

        final T proxyObject = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler);

        for (Method m : type.getMethods()) {
            try {
                final MethodInvoker<?> invoker;

                final String verb;
                if (m.getName().startsWith("get")) {
                    verb = "get";
                } else if (m.getName().startsWith("is")) {
                    verb = "is";
                } else {
                    verb = "";
                }
                
                final Class<?> returnType = m.getReturnType();
                
                Supplier defaultSupplier = () -> null;

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
                        defaultSupplier = memoize((T) config.resolve(value));
                    } else {
                        defaultSupplier = memoize(decoder.decode(returnType, config.resolve(value)));
                    }
                } 

                if (m.isDefault()) {
                    defaultSupplier = new DefaultMethodValueSupplier<T>(lookup.unreflectSpecial(m, type).bindTo(proxyObject));
                }

                final PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
                final String propName = nameAnnot != null && nameAnnot.name() != null
                                ? prefix + nameAnnot.name()
                                : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);
    
                propertyNames.put(m, propName);

                // For sub-interfaces create a proxy instance where the same proxy instance is returned but its
                // methods can still return dynamic values
                if (returnType.equals(Map.class)) {
                    invoker = createMapProperty(propName, (ParameterizedType)m.getGenericReturnType(), immutable, defaultSupplier);
                } else if (returnType.equals(Set.class)) {
                    invoker = createCollectionProperty(propName, (ParameterizedType)m.getGenericReturnType(), LinkedHashSet::new, defaultSupplier);
                } else if (returnType.equals(SortedSet.class)) {
                    invoker = createCollectionProperty(propName, (ParameterizedType)m.getGenericReturnType(), TreeSet::new, defaultSupplier);
                } else if (returnType.equals(List.class)) {
                    invoker = createCollectionProperty(propName, (ParameterizedType)m.getGenericReturnType(), ArrayList::new, defaultSupplier);
                } else if (returnType.equals(LinkedList.class)) {
                    invoker = createCollectionProperty(propName, (ParameterizedType)m.getGenericReturnType(), LinkedList::new, defaultSupplier);
                } else if (returnType.isInterface()) {
                    invoker = createInterfaceProperty(propName, newProxy(returnType, propName, immutable));
                } else if (m.getParameterTypes() != null && m.getParameterTypes().length > 0) {
                    if (nameAnnot == null) {
                        throw new IllegalArgumentException("Missing @PropertyName annotation on " + m.getDeclaringClass().getName() + "#" + m.getName());
                    }
                    invoker = createParameterizedProperty(returnType, propName, nameAnnot.name(), defaultSupplier);
                } else {
                    invoker = createScalarProperty(returnType, propName, defaultSupplier);
                }

                if (immutable) {
                    Object value = invoker.invoke(new Object[]{});
                    invokers.put(m, (args) -> value);
                } else {
                    invokers.put(m, invoker);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error proxying method " + m.getName(), e);
            }
        }
        
        return proxyObject;
    }

    private static <T> Supplier<T> memoize(T value) {
    	return () -> value;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> MethodInvoker<T> createCollectionProperty(String propName, ParameterizedType type, Supplier<Collection> collectionFactory, Supplier<T> next) {
        final Class<?> valueType = (Class<?>)type.getActualTypeArguments()[0];
        final Property<T> prop = propertyRepository
                .get(propName, String.class)
                .map(s -> {
                  if (s != null) {
                      Collection list = collectionFactory.get();
                      if (!s.isEmpty()) {
                          Arrays.asList(s.split("\\s*,\\s*")).forEach(v -> {
                              if (!v.isEmpty() || valueType == String.class) {
                                  list.add(decoder.decode(valueType, v));
                              }
                          });
                      }
                      return (T)list;
                  } else {
                      return null;
                  }
              });

        return new MethodInvoker<T>() {
			@Override
            public T invoke(Object[] args) {
                T value = prop.get();
                if (value == null) {
                    value = next.get();
                }
                if (value == null) {
                    value = (T) collectionFactory.get();
                }
                return value;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> MethodInvoker<T> createMapProperty(final String propName, final ParameterizedType type, final boolean immutable, Supplier<T> next) {
        Object valueType = type.getActualTypeArguments()[1];
        if(ParameterizedType.class.isAssignableFrom(type.getActualTypeArguments()[1].getClass())) {
            final ParameterizedType valueTypeInst = (ParameterizedType)valueType;

            // This is a map for String -> Interface so create a proxy for any value
            if (valueTypeInst.getRawType().getTypeName().equalsIgnoreCase(List.class.getCanonicalName())) {
                Map<String, List<Object>> map = new ReadOnlyMap<String, List<Object>>() {
                    Map<String, List<Object>> lookup = new ConcurrentHashMap<String, List<Object>>();

                    @Override
                    public List<Object> get(final Object key) {
                        return lookup.computeIfAbsent((String) key, new Function<String, List<Object>>() {
                            @Override
                            public List<Object> apply(String key) {
                                return (List<Object>)config.getList(propName + "." + key,(Class)valueTypeInst.getActualTypeArguments()[0]);
                            }
                        });
                    }
                };
                return (MethodInvoker<T>) createInterfaceProperty(propName, map);
            } else {
                throw new RuntimeException("The value map handler for " + valueTypeInst.getRawType().getTypeName() + " type is not implemented.");
            }

        } else {
            final Class<?> valueTypeClass = (Class<?>)valueType;
            Map<String, Object> map;
            // This is a map for String -> Interface so create a proxy for any value
            if (valueTypeClass.isInterface()) {
                map = new ReadOnlyMap<String, Object>() {
                    Map<String, Object> lookup = new ConcurrentHashMap<String, Object>();

                    @Override
                    public Object get(final Object key) {
                        return lookup.computeIfAbsent((String) key, new Function<String, Object>() {
                            @Override
                            public Object apply(String key) {
                                return newProxy(valueTypeClass, propName + "." + key, immutable);
                            }
                        });
                    }
                };
            } else {
                // This is a map of String -> DecodableType (i.e. String, Long, etc...)
                map = new ReadOnlyMap<String, Object>() {
                    @Override
                    public Object get(final Object key) {
                        return config.get(valueTypeClass, propName + "." + key);
                    }
                };
            }
            return (MethodInvoker<T>) createInterfaceProperty(propName, map, (Supplier<Map<String, Object>>)next);
        }
    }

    protected <T> Supplier<T> defaultValueFromString(Class<T> type, String defaultValue) {
        return () -> decoder.decode(type, defaultValue);
    }
    
    protected <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy, Supplier<T> next) {
        return new PropertyMethodInvoker<T>(propName, next) {
            @Override
            public T get() {
                return proxy;
            }
        };
    }

    protected <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy) {
        return (args) -> proxy;
    }

    protected <T> MethodInvoker<T> createScalarProperty(final Class<T> type, final String propName, Supplier<T> next) {
        final Property<T> prop = propertyRepository.get(propName, type);
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object[] args) {
                T result = prop.get();
                if (result == null) {
                    result = next.get();
                }
                return result;
            }
        };
    }
    
    protected <T> MethodInvoker<T> createParameterizedProperty(final Class<T> returnType, final String propName, final String nameAnnot, Supplier<T> next) {
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object[] args) {
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
                T result = getPropertyWithDefault(returnType, propName);
                if (result == null) {
                    result = next.get();
                }
                return result;
            }

            <R> R getPropertyWithDefault(Class<R> type, String propName) {
                return propertyRepository.get(propName, type).get();
            }
        }; 
    }
}
