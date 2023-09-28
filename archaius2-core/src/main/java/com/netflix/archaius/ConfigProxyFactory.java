package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyRepository;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import com.netflix.archaius.util.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
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
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    private static final Map<Type, Function<Object[], ?>> knownCollections = new HashMap<>();

    static {
        knownCollections.put(Map.class, (ignored) -> Collections.emptyMap());
        knownCollections.put(Set.class, (ignored) -> Collections.emptySet());
        knownCollections.put(SortedSet.class, (ignored) -> Collections.emptySortedSet());
        knownCollections.put(List.class, (ignored) -> Collections.emptyList());
        knownCollections.put(LinkedList.class, (ignored) -> new LinkedList<>());
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
                
                Function defaultSupplier = knownCollections.getOrDefault(returnType, (ignored) -> null);

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
                } else if (m.getParameterCount() > 0) {
                    if (nameAnnot == null) {
                        throw new IllegalArgumentException("Missing @PropertyName annotation on " + m.getDeclaringClass().getName() + "#" + m.getName());
                    }
                    invoker = createParameterizedProperty(returnType, prefix, nameAnnot.name(), defaultSupplier);
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

    private static <T> Function<Object[], T> memoize(T value) {
        return (ignored) -> value;
    }

    private static <T> Function<Object[], T> createDefaultMethodSupplier(Method method, Class<T> type, T proxyObject) {
        final MethodHandle methodHandle;

        final MethodHandles.Lookup lookup;

        try {
            if (SystemUtils.IS_JAVA_1_8) {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                        .getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                lookup = constructor.newInstance(type, MethodHandles.Lookup.PRIVATE);
                methodHandle = lookup.unreflectSpecial(method, type);
            }
            else {
                // Java 9 onwards
                lookup = MethodHandles.lookup();
                methodHandle = lookup.findSpecial(
                        method.getDeclaringClass(),
                        method.getName(),
                        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                        method.getDeclaringClass());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create temporary object for " + type.getName(), e);
        }

        if (methodHandle.type().parameterCount() == 1) {
            Function<Object, Object> getter = asFunction(lookup, methodHandle);
            //noinspection unchecked
            return (args) -> (T) getter.apply(proxyObject);
        } else if (methodHandle.type().parameterCount() == 2) {
            BiFunction<Object, Object, Object> getter = asBiFunction(lookup, methodHandle);
            return (args) -> {
                if (args == null) {
                    return null;
                }
                //noinspection unchecked
                return (T) getter.apply(proxyObject, args[0]);
            };
        }

        // Fall back to calling the MethodHandle directly
        MethodHandle boundHandle = methodHandle.bindTo(proxyObject);

        return (args) -> {
            try {
                if (args != null) {
                    //noinspection unchecked
                    return (T) boundHandle.invokeWithArguments(args);
                } else {
                    // This is a handle to a method WITH arguments, being called with none. This happens when toString()
                    // is trying to build a representation of a proxy that has a parametrized property AND the interface
                    // provides a default method for it. There's no good default to return here, so we'll just use null
                    return null;
                }
            } catch (Throwable e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                if (e instanceof Error) {
                    throw (Error) e;
                }
                throw new RuntimeException(e);
            }
        };
    }

    protected <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy) {
        LOG.debug("Creating interface property `{}` for type `{}`", propName, proxy.getClass());
        return (args) -> proxy;
    }

    protected <T> MethodInvoker<T> createScalarProperty(final Type type, final String propName, Function<Object[], T> next) {
        LOG.debug("Creating scalar property `{}` for type `{}`", propName, type.getClass());
        final Property<T> prop = propertyRepository.get(propName, type);
        return args -> {
            T value = prop.get();
            return value != null ? value : next.apply(null);
        };
    }

    protected <T> MethodInvoker<T> createParameterizedProperty(final Class<T> returnType, final String prefix, final String nameAnnot, Function<Object[], T> next) {
        LOG.debug("Creating parameterized property `{}` for type `{}`", prefix + nameAnnot, returnType);
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object[] args) {
                if (args == null) {
                    // Why would args be null if this is a parametrized property? Because toString() abuses its
                    // access to this internal representation :-/
                    // We'll fall back to trying to call the provider for the default value. That works properly if
                    // it comes from an annotation or the known collections. Our wrapper for default interface methods
                    // catches this case and just returns a null, which is probably the least bad response.
                    return next.apply(null);
                }

                // A previous version allowed the full name to be specified, even if the prefix was specified. So, for
                // backwards compatibility, we allow both including or excluding the prefix for parameterized names.
                String propName = nameAnnot;
                if (!StringUtils.isBlank(prefix) && !nameAnnot.startsWith(prefix)) {
                    propName = prefix + nameAnnot;
                }

                // Determine the actual property name by replacing with arguments using the argument index
                // to the method.  For example,
                //      @PropertyName(name="foo.${1}.${0}")
                //      String getFooValue(String arg0, Integer arg1) 
                // 
                // called as getFooValue("bar", 1) would look for the property 'foo.1.bar'
                Map<String, Object> values = Maps.newHashMap(args.length);
                for (int i = 0; i < args.length; i++) {
                    values.put(String.valueOf(i), args[i]);
                }
                propName = new StrSubstitutor(values, "${", "}", '$').replace(propName);
                T result = getPropertyWithDefault(returnType, propName);
                if (result == null) {
                    result = next.apply(args);
                }
                return result;
            }

            <R> R getPropertyWithDefault(Class<R> type, String propName) {
                return propertyRepository.get(propName, type).get();
            }
        }; 
    }

    /**
     * For a given no-args method or default method, build a Function instance that takes the instance and invokes
     * the underlying method on it.
     */
    @SuppressWarnings("unchecked")
    private static Function<Object, Object> asFunction(MethodHandles.Lookup lookup, MethodHandle methodHandle) {
        try {
            CallSite site = LambdaMetafactory.metafactory(lookup,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class),
                    methodHandle,
                    methodHandle.type());
            return (Function<Object, Object>) site.getTarget().invokeExact();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * For a given single-arg method or default method, build a Function instance that takes the instance and invokes
     * the underlying method on it.
     */
    @SuppressWarnings("unchecked")
    private static BiFunction<Object, Object, Object> asBiFunction(MethodHandles.Lookup lookup, MethodHandle methodHandle) {
        try {
            CallSite site = LambdaMetafactory.metafactory(lookup,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class, Object.class),
                    methodHandle,
                    methodHandle.type());
            return (BiFunction<Object, Object, Object>) site.getTarget().invokeExact();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }
}
