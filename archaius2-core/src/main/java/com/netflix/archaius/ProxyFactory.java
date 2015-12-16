package com.netflix.archaius;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyFactory;

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
public class ProxyFactory extends ConfigProxyFactory {
    
    @Inject
    public ProxyFactory(Decoder decoder, PropertyFactory factory) {
        super(decoder, factory);
    }
}
