package com.netflix.archaius.bridge;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.commons.CommonsToConfig;
import com.netflix.config.ConfigurationManager;

/**
 * Install this module so that an injectable {@link ConfigProxyFactory} can be backed by the static
 * Archaius1 {@link ConfigurationManager}.  Use this for legacy applications that use Archaius1 and
 * haven't yet update to use Archaius2 as its configuration solution.
 */
public final class Archaius2BackportModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides 
    @Singleton
    ConfigProxyFactory getConfigProxyFactory() {
        return new ConfigProxyFactory(new CommonsToConfig(ConfigurationManager.getConfigInstance()));
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
