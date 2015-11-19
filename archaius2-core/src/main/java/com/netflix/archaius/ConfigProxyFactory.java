package com.netflix.archaius;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyFactory;

/**
 * Factory for binding a configuration interface to properties in a PropertyFactory
 * instance.  Getter methods on the interface are mapped by naming convention
 * by the property name may be overridden using the @PropertyName annotation.
 * 
 * Note that an application should normally have just one instance of ConfigProxyFactory
 * and PropertyFactory since PropertyFactory caches {@link com.netflix.archaius.api.Property} objects.
 * 
 * @author elandau
 */
public class ConfigProxyFactory extends ProxyFactory {

    public ConfigProxyFactory(Decoder decoder, PropertyFactory factory) {
        super(decoder, factory);
    }

    public <T> T newProxy(final Class<T> type, final String initialPrefix) {
        return super.newProxy(type, initialPrefix);
    }
        
    public <T> T newProxy(final Class<T> type) {
        return super.newProxy(type);
    }

}
