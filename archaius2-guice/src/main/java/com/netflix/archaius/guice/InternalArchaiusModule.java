package com.netflix.archaius.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigLoader;
import com.netflix.archaius.api.ConfigReader;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyRepository;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.DefaultLayer;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.readers.PropertiesConfigReader;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Named;
import jakarta.inject.Provider;

final class InternalArchaiusModule extends AbstractModule {
    static final String CONFIG_NAME_KEY         = "archaius.config.name";
    
    private static final String DEFAULT_CONFIG_NAME     = "application";
    
    private static final String RUNTIME_LAYER_NAME      = "RUNTIME";
    private static final String REMOTE_LAYER_NAME       = "REMOTE";
    private static final String SYSTEM_LAYER_NAME       = "SYSTEM";
    private static final String ENVIRONMENT_LAYER_NAME  = "ENVIRONMENT";
    private static final String APPLICATION_LAYER_NAME  = "APPLICATION";
    private static final String LIBRARIES_LAYER_NAME    = "LIBRARIES";
    private static final String DEFAULT_LAYER_NAME      = "DEFAULT";
    
    private static AtomicInteger uniqueNameCounter = new AtomicInteger();

    private static String getUniqueName(String prefix) {
        return prefix +"-" + uniqueNameCounter.incrementAndGet();
    }
    
    @Override
    protected void configure() {
        ConfigurationInjectingListener listener = new ConfigurationInjectingListener();
        requestInjection(listener);
        bind(ConfigurationInjectingListener.class).toInstance(listener);
        requestStaticInjection(ConfigurationInjectingListener.class);
        bindListener(Matchers.any(), listener);
        
        Multibinder.newSetBinder(binder(), ConfigReader.class)
            .addBinding().to(PropertiesConfigReader.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    @RuntimeLayer
    SettableConfig getSettableConfig() {
        return new DefaultSettableConfig();
    }
    
    @Provides
    @Singleton
    @LibrariesLayer
    CompositeConfig getLibrariesLayer() {
        return new DefaultCompositeConfig();
    }
    
    @Singleton
    private static class ConfigParameters {
        @Inject(optional=true)
        @Named(CONFIG_NAME_KEY)
        String configName;
        
        @Inject
        @RuntimeLayer
        SettableConfig  runtimeLayer;
        
        @Inject
        @LibrariesLayer
        CompositeConfig librariesLayer;
        
        @Inject(optional=true)
        @RemoteLayer 
        Provider<Config> remoteLayerProvider;
        
        @Inject(optional=true)
        @DefaultLayer 
        Set<Config> defaultConfigs;
        
        @Inject(optional=true)
        @ApplicationOverride
        Config applicationOverride;

        @Inject(optional =true)
        @ApplicationOverrideResources
        Set<String> overrideResources;
        
        boolean hasApplicationOverride() {
            return applicationOverride != null;
        }
        
        boolean hasDefaultConfigs() {
            return defaultConfigs != null;
        }

        boolean hasRemoteLayer() {
            return remoteLayerProvider != null;
        }

        boolean hasOverrideResources() {
            return overrideResources != null;
        }
        
        String getConfigName() {
            return configName == null ? DEFAULT_CONFIG_NAME : configName;
        }
    }

    @Provides
    @Singleton
    @Raw
    CompositeConfig getRawCompositeConfig() throws Exception {
        return new DefaultCompositeConfig();
    }

    @Provides
    @Singleton
    @Raw
    Config getRawConfig(@Raw CompositeConfig config) throws Exception {
        return config;
    }

    @Provides
    @Singleton
    Config getConfig(ConfigParameters params, @Raw CompositeConfig config, ConfigLoader loader) throws Exception {
        CompositeConfig applicationLayer = new DefaultCompositeConfig();
        CompositeConfig remoteLayer = new DefaultCompositeConfig();
        
        config.addConfig(RUNTIME_LAYER_NAME,      params.runtimeLayer);
        config.addConfig(REMOTE_LAYER_NAME,       remoteLayer);
        config.addConfig(SYSTEM_LAYER_NAME,       SystemConfig.INSTANCE);
        config.addConfig(ENVIRONMENT_LAYER_NAME,  EnvironmentConfig.INSTANCE);
        config.addConfig(APPLICATION_LAYER_NAME,  applicationLayer);
        config.addConfig(LIBRARIES_LAYER_NAME,    params.librariesLayer);
        
        // Load defaults layer
        if (params.hasDefaultConfigs()) {
            CompositeConfig defaultLayer = new DefaultCompositeConfig();
            config.addConfig(DEFAULT_LAYER_NAME,      defaultLayer);
            for (Config c : params.defaultConfigs) {
                defaultLayer.addConfig(getUniqueName("default"), c);
            }
        }

        if (params.hasOverrideResources()) {
            for (String resourceName : params.overrideResources) {
                applicationLayer.addConfig(resourceName, loader.newLoader().load(resourceName));
            }
        }

        if (params.hasApplicationOverride()) {
            applicationLayer.addConfig(getUniqueName("override"), params.applicationOverride);
        }
        
        applicationLayer.addConfig(params.getConfigName(), loader
                .newLoader()
                .load(params.getConfigName()));

        // Load remote properties
        if (params.hasRemoteLayer()) {
            remoteLayer.addConfig(getUniqueName("remote"), params.remoteLayerProvider.get());
        }
        
        return config;
    }
        
    @Singleton
    private static class OptionalCascadeStrategy {
        @Inject(optional=true)
        CascadeStrategy       cascadingStrategy;
        
        CascadeStrategy get() {
            return cascadingStrategy == null ? new NoCascadeStrategy() : cascadingStrategy;
        }
    }
    
    @Provides
    @Singleton
    ConfigLoader getLoader(
            @Raw                  CompositeConfig rawConfig,
            Set<ConfigReader>     readers,
            OptionalCascadeStrategy  cascadeStrategy
            ) throws ConfigException {
        
        return DefaultConfigLoader.builder()
            .withConfigReaders(readers)
            .withDefaultCascadingStrategy(cascadeStrategy.get())
            .withStrLookup(ConfigStrLookup.from(rawConfig))
            .build();
    }
    
    @Provides
    @Singleton
    Decoder getDecoder() {
        return DefaultDecoder.INSTANCE;
    }

    @Provides
    @Singleton
    PropertyFactory getPropertyFactory(Config config) {
        return DefaultPropertyFactory.from(config);
    }
    
    @Provides
    @Singleton
    PropertyRepository getPropertyRespository(PropertyFactory propertyFactory) {
        return propertyFactory;
    }

    @Provides
    @Singleton
    ConfigProxyFactory getProxyFactory(Config config, Decoder decoder, PropertyFactory factory) {
        return new ConfigProxyFactory(config, decoder, factory);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return InternalArchaiusModule.class.equals(obj.getClass());
    }
}
