package com.netflix.archaius;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

/**
 * Factory for binding a configuration interface to properties in a Config
 * instance.  Getter methods on the interface are mapped by naming convention
 * by the property name may be overridden using the @PropertyName annotation.
 * 
 * @author elandau
 * @deprecated Use {@link ConfigProxyFactory} instead
 */
@Singleton
@Deprecated
public class ProxyFactory {
    
    /**
     * The decoder is used for the purpose of decoding any @DefaultValue annotation
     */
    private final Decoder decoder;
    private final PropertyFactory propertyFactory;
    
    @Inject
    public ProxyFactory(Decoder decoder, PropertyFactory factory) {
        this.decoder = decoder;
        this.propertyFactory = factory;
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
     * @author elandau
     *
     * @param <T>
     */
    private static interface MethodInvoker<T> {
        /**
         * Invoke the method with the provided arguments
         * @param args
         * @return
         */
        T invoke(Object[] args);

        /**
         * Return the property key
         * @return
         */
        String getKey();
    }
    
    /**
     * Abstract method invoker that encapsulates a property
     * @author elandau
     *
     * @param <T>
     */
    private static abstract class PropertyMethodInvoker<T> extends AbstractProperty<T> implements MethodInvoker<T> {
        public PropertyMethodInvoker(String key) {
            super(key);
        }
        
        @Override
        public T invoke(Object[] args) {
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
            final String verb;
            if (m.getName().startsWith("get")) {
                verb = "get";
            }
            else if (m.getName().startsWith("is")) {
                verb = "is";
            }
            else {
                verb = "";
            }
            
            final DefaultValue defaultValue = m.getAnnotation(DefaultValue.class);
            final Class<?> returnType = m.getReturnType();
            final PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
            final String propName = nameAnnot != null && nameAnnot.name() != null
                            ? prefix + nameAnnot.name()
                            : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);

            // For sub-interfaces create a proxy instance where the same proxy instance is returned but its
            // methods can still return dynamic values
            if (returnType.isInterface()) {
                invokers.put(m, createInterfaceProperty(propName, newProxy(returnType, propName, immutable)));
            }
            else {
                if (m.getParameterTypes().length > 0) {
                    invokers.put(m, new MethodInvoker() {
                        @Override
                        public Object invoke(Object[] args) {
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
                            String propName = new StrSubstitutor(values, "${", "}", '$').replace(nameAnnot.name());
                            
                            // Read the actual value now that the proeprty name is known.  Note that we can't create
                            // 
                            if (defaultValue != null) {
                                return getPropertyWithDefault(returnType, propName, defaultValue.value());
                            }
                            else {
                                return propertyFactory.getConfig().get(returnType, propName, null);
                            }
                        }

                        <R> R getPropertyWithDefault(Class<R> type, String propName, String defaultValue) {
                            return propertyFactory.getConfig().get(type, propName, decoder.decode(type, defaultValue));
                        }

                        @Override
                        public String getKey() {
                            return propName;
                        }
                    });
                }
                else if (immutable) {
                    if (defaultValue != null) {
                        invokers.put(m, createImmutablePropertyWithDefault(m.getReturnType(), propName, defaultValue.value()));
                    }
                    else {
                        invokers.put(m, createImmutablePropertyWithDefault(m.getReturnType(), propName, null));
                    }
                }
                else {
                    if (defaultValue != null) {
                        invokers.put(m, createDynamicProperty(m.getReturnType(), propName, defaultValue.value()));
                    } 
                    else {
                        invokers.put(m, createDynamicProperty(m.getReturnType(), propName, null));
                    }
                }
            }
        }
        
        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                MethodInvoker<?> invoker = invokers.get(method);
                if (invoker != null) {
                    return invoker.invoke(args);
                }
                
                if ("toString".equals(method.getName())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(type.getSimpleName()).append("[");
                    Iterator<Entry<Method, MethodInvoker<?>>> iter = invokers.entrySet().iterator();
                    while (iter.hasNext()) {
                        MethodInvoker entry = iter.next().getValue();
                        sb.append(entry.getKey().substring(prefix.length())).append("='");
                        try {
                            sb.append(entry.invoke(null));
                        }
                        catch (Exception e) {
                            sb.append(e.getMessage());
                        }
                        sb.append("'");
                        if (iter.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");
                    return sb.toString();
                }
                else {
                    throw new NoSuchMethodError(method.getName() + " not found on interface " + type.getName());
                }
            }
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler);
    }
    
    private <T> MethodInvoker<T> createImmutablePropertyWithDefault(final Class<T> type, final String propName, final String defaultValue) {
        return new PropertyMethodInvoker<T>(propName) {
            private volatile T cached;
            
            @Override
            public T get() {
                if (cached == null) {
                    cached = propertyFactory.getConfig().get(type, propName, decoder.decode(type, defaultValue));
                }
                return cached;
            }
        };
    }
    
    private <T> MethodInvoker<T> createImmutableProperty(final Class<T> type, final String propName) {
        return new PropertyMethodInvoker<T>(propName) {
            private volatile T cached;
            @Override
            public T get() {
                if (cached == null) {
                    cached = propertyFactory.getConfig().get(type, propName);
                }
                return cached;
            }
        };
    }
    
    private <T> MethodInvoker<T> createInterfaceProperty(String propName, final T proxy) {
        return new PropertyMethodInvoker<T>(propName) {
            @Override
            public T get() {
                return proxy;
            }
        };
    }

    private <T> MethodInvoker<T> createDynamicProperty(final Class<T> type, final String propName, final String defaultValue) {
        final Property<T> prop = propertyFactory.getProperty(propName).asType(type, defaultValue != null ? decoder.decode(type, defaultValue) : null);
        return new MethodInvoker<T>() {
            @Override
            public T invoke(Object[] args) {
                return prop.get();
            }

            @Override
            public String getKey() {
                return prop.getKey();
            }
        };
    }
}
