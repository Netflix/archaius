package com.netflix.archaius;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.annotations.DefaultValue;

/**
 * Factory for creating a Proxy instance that is bound to configuration.
 * Getter methods on the interface are mapped by naming convention.
 * 
 * @author elandau
 *
 */
public class ProxyFactory {
    
    private final Decoder decoder;

    public ProxyFactory() {
        this(DefaultDecoder.INSTANCE);
    }
    
    @Inject
    public ProxyFactory(Decoder decoder) {
        this.decoder = decoder;
    }
    
    // TODO: Add method nexProxy(Class<T> type, Config config);
    
    /**
     * Create a proxy for the provided interface type for which all getter methods are bound
     * to a Property.
     * 
     * @param type
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T newProxy(Class<T> type, PropertyFactory factory) {
        Configuration annot = type.getAnnotation(Configuration.class);
        
        String prefix = annot == null 
                      ? "" 
                      : !annot.prefix().isEmpty() && !annot.prefix().endsWith(".")
                          ? annot.prefix() + "."
                          : annot.prefix();
        
        // Iterate through all declared methods of the class looking for setter methods.
        // Each setter will be mapped to a Property<T> for the property name:
        //      prefix + lowerCamelCaseDerivedPropertyName
        final Map<Method, Property<?>> properties = new HashMap<Method, Property<?>>();
        for (Method m : type.getDeclaredMethods()) {
            if (!m.getName().startsWith("get")) {
                continue;
            }
            
            Object defaultValue = null;
            DefaultValue annotDefaultValue = m.getAnnotation(DefaultValue.class);
            Class<?> returnType = m.getReturnType();
            if (annotDefaultValue != null) {
                try {
                    defaultValue = decoder.decode(returnType, annotDefaultValue.value());
                } catch (Exception e) {
                    throw new RuntimeException("No accessible valueOf(String) method to parse default value for type " + returnType.getName(), e);
                }
            }
            
            if (returnType.isPrimitive() && defaultValue == null) {
                throw new RuntimeException("Method with primite return type must have a @DefaultValue.  method=" + m.getName());
            }
            
            // TODO: default value
            // TODO: sub proxy for non-primitive types
            String propName = prefix + Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            properties.put(m, factory.getProperty(propName).asType((Class)m.getReturnType(), defaultValue));
        }
        
        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return properties.get(method).get();
            }
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler);
    }
}
