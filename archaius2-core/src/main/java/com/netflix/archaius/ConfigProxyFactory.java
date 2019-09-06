package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyRepository;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private static final Logger LOG = LoggerFactory.getLogger(ConfigProxyFactory.class);

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
    interface MethodInvoker<T> {
        /**
         * Invoke the method with the provided arguments
         * @param args
         * @return
         */
        T invoke(Object[] args);
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

    private static Map<Type, Supplier<?>> knownCollections = new HashMap<>();

    static {
        knownCollections.put(Map.class, Collections::emptyMap);
        knownCollections.put(Set.class, Collections::emptySet);
        knownCollections.put(SortedSet.class, Collections::emptySortedSet);
        knownCollections.put(List.class, Collections::emptyList);
        knownCollections.put(LinkedList.class, LinkedList::new);
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
            if ("equals".equals(method.getName())) {
            	return proxy == args[0];
            }
            else if ("hashCode".equals(method.getName())) {
            	return System.identityHashCode(proxy);
            }
            else if ("toString".equals(method.getName())) {
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
                
                Supplier defaultSupplier = knownCollections.getOrDefault(returnType, () -> null);
                
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
                    defaultSupplier = createDefaultMethodSupplier(m, type, proxyObject);
                }

                final PropertyName nameAnnot = m.getAnnotation(PropertyName.class);
                final String propName = nameAnnot != null && nameAnnot.name() != null
                                ? prefix + nameAnnot.name()
                                : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);

                propertyNames.put(m, propName);

                if (!knownCollections.containsKey(returnType) && returnType.isInterface()) {
                    invoker = createInterfaceProperty(propName, newProxy(returnType, propName, immutable));
                } else if (m.getParameterTypes() != null && m.getParameterTypes().length > 0) {
                    if (nameAnnot == null) {
                        throw new IllegalArgumentException("Missing @PropertyName annotation on " + m.getDeclaringClass().getName() + "#" + m.getName());
                    }
                    invoker = createParameterizedProperty(returnType, propName, nameAnnot.name(), defaultSupplier);
                } else {
                    invoker = createScalarProperty(m.getGenericReturnType(), propName, defaultSupplier);
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

    private static <T> Supplier<T> createDefaultMethodSupplier(Method method, Class<T> type, T proxyObject) {
        final MethodHandle methodHandle;

        try {
            if (SystemUtils.IS_JAVA_1_8) {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                        .getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                methodHandle = constructor.newInstance(type, MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, type)
                        .bindTo(proxyObject);
            }
            else {
                // Java 9 onwards
                methodHandle = MethodHandles.lookup()
                        .findSpecial(type,
                                method.getName(),
                                MethodType.methodType(method.getReturnType(), new Class[0]),
                                type)
                        .bindTo(proxyObject);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create temporary object for " + type.getName(), e);
        }

        return () -> {
            try {
                //noinspection unchecked
                return (T) methodHandle.invokeWithArguments();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy) {
        LOG.debug("Creating interface property `{}` for type `{}`", propName, proxy.getClass());
        return (args) -> proxy;
    }

    protected <T> MethodInvoker<T> createScalarProperty(final Type type, final String propName, Supplier<T> next) {
        LOG.debug("Creating scalar property `{}` for type `{}`", propName, type.getClass());
        final Property<T> prop = propertyRepository.get(propName, type);
        return args -> Optional.ofNullable(prop.get()).orElseGet(next);
    }
    
    protected <T> MethodInvoker<T> createParameterizedProperty(final Class<T> returnType, final String propName, final String nameAnnot, Supplier<T> next) {
        LOG.debug("Creating parameterized property `{}` for type `{}`", propName, returnType);
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
