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
    @SuppressWarnings("unchecked")
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
        
        final String prefix = derivePrefix(annot, initialPrefix);
        
        // Iterate through all declared methods of the class looking for setter methods.
        // Each setter will be mapped to a Property<T> for the property name:
        //      prefix + lowerCamelCaseDerivedPropertyName
        final Map<Method, Property<?>> properties = new HashMap<Method, Property<?>>();
        for (Method m : type.getDeclaredMethods()) {
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
            
            PropertyName nameAnnot = m.getAnnotation(PropertyName.class); 
            String propName = nameAnnot != null && nameAnnot.name() != null
                            ? prefix + nameAnnot.name()
                            : prefix + Character.toLowerCase(m.getName().charAt(verb.length())) + m.getName().substring(verb.length() + 1);

            if (returnType.isInterface()) {
                properties.put(m, new ImmutableProperty(propName, newProxy(returnType, propName)));
            }
            else {
                Property prop = propertyFactory.getProperty(propName).asType((Class)m.getReturnType(), defaultValue);
                properties.put(m, annot != null && annot.immutable() ? new ImmutableProperty(propName, prop.get()) : prop);
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
                        sb.append(entry.getKey().substring(prefix.length())).append("=").append(entry.get());
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
}
