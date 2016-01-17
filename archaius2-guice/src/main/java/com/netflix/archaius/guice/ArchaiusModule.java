/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.guice;

import java.util.Properties;
import java.util.Set;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.readers.PropertiesConfigReader;

/**
 * Guice Module for setting up archaius and making its components injectable.  Installing this
 * module also enables the following functionality.
 * 
 * <ul>
 * <li>Injectable Config</li>
 * <li>Configuration Proxy</li>
 * <li>Configuration mapping</li>
 * </uL>
 * 
 * Note that this module has state and should therefore only be installed once.  It should only
 * be installed by applications and not by libraries depending on archaius.  Such libraries 
 * should only specify a bindger.requiresBinding(Config.class).  
 * 
 * This module creates an injectable Config instances that has the following override structure in
 * order of precedence. 
 * 
 *  RUNTIME     - properties set from code
 *  REMOTE      - properties loaded from a remote source
 *  SYSTEM      - System properties
 *  ENVIRONMENT - Environment properties
 *  APPLICATION - Configuration loaded by the application
 *  LIBRARIES   - Configuration loaded by libraries used by the application
 * 
 * Runtime properties may be set in code by injecting and calling one of the setXXX methods of,
 *  {@literal @}RuntimeLayer SettableConfig config
 *  
 * A remote configuration may be specified by binding to {@literal @}RemoteLayer Config
 * When setting up a remote configuration that needs access to Archaius's Config
 * make sure to inject the qualified {@literal @}Raw Config otherwise the injector will fail
 * with a circular dependency error.  Note that the injected config will have 
 * system, environment and application properties loaded into it.
 * 
 * <code>
 * public class FooRemoteModule extends AbstractModule {
 *     {@literal @}Override
 *     protected void configure() {}
 *     
 *     {@literal @}Provides
 *     {@literal @}RemoteLayer
 *     Config getRemoteConfig({@literal @}Raw Config config) {
 *         return new FooRemoteConfigImplementaiton(config);
 *     }
 * }
 * </code>
 * 
 */
public final class ArchaiusModule extends AbstractModule {
    private static final String DEFAULT_CONFIG_NAME     = "application";
    
    private static final String RUNTIME_LAYER_NAME      = "RUNTIME";
    private static final String REMOTE_LAYER_NAME       = "REMOTE";
    private static final String SYSTEM_LAYER_NAME       = "SYSTEM";
    private static final String ENVIRONMENT_LAYER_NAME  = "ENVIRONMENT";
    private static final String APPLICATION_LAYER_NAME  = "APPLICATION";
    private static final String LIBRARIES_LAYER_NAME    = "LIBRARIES";
    private static final String DEFAULT_LAYER_NAME      = "DEFAULT";
    
    // These are here to force using the backwards compatibility bridge for 
    // Archaius1's static API
    static {
        System.setProperty("archaius.default.configuration.class",      "com.netflix.archaius.bridge.StaticAbstractConfiguration");
        System.setProperty("archaius.default.deploymentContext.class",  "com.netflix.archaius.bridge.StaticDeploymentContext");
    }
    
    private String configName = DEFAULT_CONFIG_NAME;
    
    private Class<? extends CascadeStrategy> cascadeStrategy = null;
    private Config applicationOverride;
    private int uniqueNameCounter = 0;

    public ArchaiusModule withConfigName(String value) {
        this.configName = value;
        return this;
    }
    
    public ArchaiusModule withApplicationOverrides(Properties prop) {
        return withApplicationOverrides(MapConfig.from(prop));
    }
    
    public ArchaiusModule withApplicationOverrides(Config config) {
        applicationOverride = config;
        return this;
    }
  
    /**
     * @deprecated  Customize by binding CascadeStrategy in a guice module
     */
    @Deprecated
    public ArchaiusModule withCascadeStrategy(Class<? extends CascadeStrategy> cascadeStrategy) {
        this.cascadeStrategy = cascadeStrategy;
        return this;
    }
    
    private String getUniqueName(String prefix) {
        uniqueNameCounter++;
        return prefix +"-" + uniqueNameCounter;
    }
    
    @Override
    protected void configure() {
        bindListener(Matchers.any(), new ConfigurationInjectingListener());
        
        Multibinder.newSetBinder(binder(), ConfigReader.class)
            .addBinding().to(PropertiesConfigReader.class).in(Scopes.SINGLETON);
        
        if (cascadeStrategy != null) {
            bind(CascadeStrategy.class).to(cascadeStrategy);
        }

        if (applicationOverride != null) {
            bind(Config.class).annotatedWith(ApplicationOverride.class).toInstance(applicationOverride);
        }
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
        if (params.defaultConfigs != null) {
            CompositeConfig defaultLayer = new DefaultCompositeConfig();
            config.addConfig(DEFAULT_LAYER_NAME,      defaultLayer);
            for (Config c : params.defaultConfigs) {
                defaultLayer.addConfig(getUniqueName("default"), c);
            }
        }
        
        // Load application properties
        if (applicationOverride != null) {
            applicationLayer.addConfig(getUniqueName("override"), applicationOverride);
        }
        
        applicationLayer.addConfig(configName, loader
                .newLoader()
                .load(configName));

        // Load remote properties
        if (params.remoteLayerProvider != null) {
            remoteLayer.addConfig(getUniqueName("remote"), params.remoteLayerProvider.get());
        }
        
        return config;
    }
        
    @Singleton
    private static class OptionalLoaderConfig {
        @Inject(optional=true)
        CascadeStrategy       cascadingStrategy;
    }
    
    @Provides
    @Singleton
    ConfigLoader getLoader(
            @Raw                  CompositeConfig rawConfig,
            Set<ConfigReader>     readers,
            OptionalLoaderConfig  optional
            ) throws ConfigException {
        
        return DefaultConfigLoader.builder()
            .withConfigReaders(readers)
            .withDefaultCascadingStrategy(optional.cascadingStrategy == null ? new NoCascadeStrategy() : optional.cascadingStrategy)
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
    ConfigProxyFactory getProxyFactory(Decoder decoder, PropertyFactory factory) {
        return new ConfigProxyFactory(decoder, factory);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * equals() on a Module is used by Guice to dedup modules that are installed more than 
     * once as modules are assumed to be stateless.  ArchaiusModule however is stateful in
     * that it is initially set up outside of Guice.  The following equals() will
     * result in duplicate binding errors if more than one ArchaiusModule is installed().
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
