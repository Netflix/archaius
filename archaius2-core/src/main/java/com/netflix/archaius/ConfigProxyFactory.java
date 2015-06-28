package com.netflix.archaius;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory for binding a configuration interface to properties in a PropertyFactory
 * instance.  Getter methods on the interface are mapped by naming convention
 * by the property name may be overridden using the @PropertyName annotation.
 * 
 * Note that an application should normally have just one instance of ConfigProxyFactory
 * and PropertyFactory since PropertyFactory caches {@link Property} objects.
 * 
 * @author elandau
 */
@Singleton
public class ConfigProxyFactory extends ProxyFactory {

    @Inject
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
