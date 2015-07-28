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

import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.annotations.DefaultValue;
import com.netflix.archaius.annotations.PropertyName;

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
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    <T> T newProxy(final Class<T> type, final String initialPrefix, boolean immutable) {
        Configuration annot = type.getAnnotation(Configuration.class);
        
        final String prefix = derivePrefix(annot, initialPrefix);
        
        // Iterate through all declared methods of the class looking for setter methods.
        // Each setter will be mapped to a Property<T> for the property name:
        //      prefix + lowerCamelCaseDerivedPropertyName
        final Map<Method, Property<?>> properties = new HashMap<>();
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
            
            DefaultValue annotDefaultValue = m.getAnnotation(DefaultValue.class);
            Class<?> returnType = m.getReturnType();
            
            PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
            String propName = nameAnnot != null && nameAnnot.name() != null
                            ? prefix + nameAnnot.name()
                            : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);

            // For sub-interfaces create a proxy instance where the same proxy instance is returned but its
            // methods can still return dynamic values
            if (returnType.isInterface()) {
                properties.put(m, createInterfaceProperty(propName, newProxy(returnType, propName, immutable)));
            }
            else {
                
                if (immutable) {
                    if (annotDefaultValue == null) {
                        properties.put(m, createImmutableProperty(propName, m.getReturnType()));
                    }
                    else {
                        properties.put(m, createImmutablePropertyWithDefault(propName, m.getReturnType(), annotDefaultValue.value()));
                    }
                }
                else {
                    if (annotDefaultValue == null) {
                        properties.put(m, new RequiredProperty(createDynamicProperty(propName, m.getReturnType(), null)));
                    }
                    else {
                        properties.put(m, createDynamicProperty(propName, m.getReturnType(), annotDefaultValue.value()));
                    }
                }
            }
        }
        
        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Property prop = properties.get(method);
                if (prop != null) {
                    return prop.get();
                }
                else if ("toString".equals(method.getName())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(type.getSimpleName()).append("[");
                    Iterator<Entry<Method, Property<?>>> iter = properties.entrySet().iterator();
                    while (iter.hasNext()) {
                        Property entry = iter.next().getValue();
                        sb.append(entry.getKey().substring(prefix.length())).append("='");
                        try {
                            sb.append(entry.get());
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
    
    private <T> Property<T> createImmutablePropertyWithDefault(final String propName, final Class<T> type, final String defaultValue) {
        return new AbstractProperty<T>(propName) {
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
    
    private <T> Property<T> createImmutableProperty(final String propName, final Class<T> type) {
        return new AbstractProperty<T>(propName) {
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
    
    private <T> Property<T> createInterfaceProperty(String propName, final T proxy) {
        return new AbstractProperty<T>(propName) {
            @Override
            public T get() {
                return proxy;
            }
        };
    }

    private <T> Property<T> createDynamicProperty(final String propName, final Class<T> type, final String defaultValue) {
        return propertyFactory.getProperty(propName).asType(type, defaultValue != null ? decoder.decode(type, defaultValue) : null);
    }

    public static class RequiredProperty<T> extends DelegatingProperty<T> {
        public RequiredProperty(Property<T> prop) {
            super(prop);
        }

        @Override
        public T get() {
            T value = delegate.get();
            if (value == null) {
                throw new RuntimeException("Missing value for property " + getKey());
            }
            return value;
        }
    }
}
