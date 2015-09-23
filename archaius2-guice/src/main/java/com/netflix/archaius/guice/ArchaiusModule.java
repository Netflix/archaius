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

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.ConfigLoader;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.ConfigReader;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.CompositeConfig.Builder;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigAlreadyExistsException;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.inject.ApplicationLayer;
import com.netflix.archaius.inject.DefaultsLayer;
import com.netflix.archaius.inject.LibrariesLayer;
import com.netflix.archaius.inject.RemoteLayer;
import com.netflix.archaius.inject.RuntimeLayer;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.readers.PropertiesConfigReader;

/**
 * Guice module with default bindings to enable Config injection and 
 * configuration mapping/binding in a Guice based application.
 * 
 * By default this module will create a top level Config that is a 
 * CompositeConfig of the following layers,
 * RUNTIME     - properties set from code
 * OVERRIDE    - properties loaded from a remote source
 * SYSTEM      - System properties
 * ENVIRONMENT - Environment properties
 * APPLICATION - Configuration loaded by the application
 * LIBRARIES   - Configuration loaded by libraries used by the application
 * DEFAULTS    - Defaults that can be set from code
 * 
 * Runtime properties may be set by either injecting and calling one of the
 * setters for,
 *  @RuntimeLayer SettableConfig config
 * or at startup by specifying a multibinding
 *  Multibinder.newSetBinder(binder(), ConfigResolver.class, RuntimeLayer.class)...
 *  
 * Override properties may be set by either injecting 
 *  @OverrideLayer CompositeConfig config
 * or at startup by specifying a multibinding
 *  Multibinder.newSetBinder(binder(), ConfigResolver.class, OverrideLayer.class)...
 * 
 * Application properties will be loaded automatically from resources with the base 
 * name indicated by the binding to @ApplicationLayer String.  The default value
 * is 'application' be can be changed via a binding override.
 * 
 * @author elandau
 */
public final class ArchaiusModule extends AbstractModule {
    
    private static final String RUNTIME_LAYER_NAME              = "RUNTIME";
    private static final String REMOTE_LAYER_NAME               = "REMOTE";
    private static final String SYSTEM_LAYER_NAME               = "SYSTEM";
    private static final String ENVIRONMENT_LAYER_NAME          = "ENVIRONMENT";
    private static final String APPLICATION_OVERRIDE_LAYER_NAME = "APPLICATION_OVERRIDE";
    private static final String APPLICATION_LAYER_NAME          = "APPLICATION";
    private static final String LIBRARIES_LAYER_NAME            = "LIBRARIES";
    private static final String DEFAULTS_LAYER_NAME             = "DEFAULTS";
    
    private static final AtomicInteger idCounter = new AtomicInteger();
    
    @Override
    protected void configure() {
        bindListener(Matchers.any(), new ConfigurationInjectingListener());

        Multibinder.newSetBinder(binder(), ConfigReader.class)
            .addBinding().to(PropertiesConfigReader.class).in(Scopes.SINGLETON);
        
        bind(ArchaiusConfiguration.class).to(OptionalArchaiusConfiguration.class);
    }
    
    @Provides
    @Singleton
    @RuntimeLayer
    SettableConfig getSettableConfig() {
        return new DefaultSettableConfig();
    }
    
    @Provides
    @Singleton
    @DefaultsLayer
    SettableConfig getDefaultsConfig() {
        return new DefaultSettableConfig();
    }
    
    @Provides
    @Singleton
    @ApplicationLayer 
    CompositeConfig getApplicationLayer() {
        return new CompositeConfig();
    }

    @Provides
    @Singleton
    @LibrariesLayer
    CompositeConfig getLibrariesLayer() {
        return new CompositeConfig();
    }
    
    @Provides
    @Singleton
    @RemoteLayer
    CompositeConfig getRemoteLayer() {
        return new CompositeConfig();
    }
    
    @Provides
    @Singleton
    @RemoteLayer
    Config getRemoteLayer(@RemoteLayer CompositeConfig config) {
        return config;
    }
    
    /**
     * This is the main config loader for the application.
     * 
     * @param rootConfig
     * @param defaultStrategy
     * @param readers
     * @return
     */
    @Provides
    @Singleton
    ConfigLoader getLoader(
            @RootLayer            Config config,
            ArchaiusConfiguration archaiusConfiguration,
            Set<ConfigReader>     readers
            ) {
        return DefaultConfigLoader.builder()
                .withConfigReader(readers)
                .withDefaultCascadingStrategy(archaiusConfiguration.getCascadeStrategy())
                .withFailOnFirst(false)
                .withStrLookup(ConfigStrLookup.from(config))
                .build();
    }

    /**
     * The internal RootLayer is meant only to construct the override hierarchy and 
     * not to load or initialize any of the layers.  Initialization of layers is done
     * is getConfig()
     * 
     * @param archaiusConfiguration
     * @param settableLayer
     * @param overrideLayer
     * @param applicationLayer
     * @param librariesLayer
     * @param defaultsLayer
     * @return
     * @throws ConfigException
     */
    @Provides
    @Singleton
    @RootLayer
    Config getInternalConfig(
            ArchaiusConfiguration archaiusConfiguration,
            @RuntimeLayer             SettableConfig    settableLayer,
            @RemoteLayer              Config            overrideLayer,
            @ApplicationLayer         CompositeConfig   applicationLayer, 
            @LibrariesLayer           CompositeConfig   librariesLayer,
            @DefaultsLayer            SettableConfig    defaultsLayer) throws ConfigException {
        Builder builder = CompositeConfig.builder()
                .withConfig(RUNTIME_LAYER_NAME,              settableLayer)
                .withConfig(REMOTE_LAYER_NAME,               overrideLayer)
                .withConfig(SYSTEM_LAYER_NAME,               SystemConfig.INSTANCE)
                .withConfig(ENVIRONMENT_LAYER_NAME,          EnvironmentConfig.INSTANCE);
        
        if (archaiusConfiguration.getApplicationOverride() != null) {
            builder.withConfig(APPLICATION_OVERRIDE_LAYER_NAME, archaiusConfiguration.getApplicationOverride());
        }
        builder.withConfig(APPLICATION_LAYER_NAME,          applicationLayer)
                .withConfig(LIBRARIES_LAYER_NAME,            librariesLayer)
                .withConfig(DEFAULTS_LAYER_NAME,             defaultsLayer)
                ;
        
        return builder.build();
    }
    
    /**
     * All code will ultimately inject Config to gain access to the entire 
     * configuration hierarchy.  The empty hierarchy is created by @RootLayer
     * but here we do the actual Configuration initialization which includes,
     * 1.  Loading application properties
     * 2.  Loading runtime overrides
     * 3.  Loading override layer overrides 
     * 
     * @param librariesLayer
     * @param applicationLayer
     * @param settableLayer
     * @param remoteLayer
     * @return
     * @throws ConfigException
     */
    @Provides
    @Singleton
    public Config getConfig(
            ArchaiusConfiguration archaiusConfiguration,
            @RootLayer        Config            config,
            @ApplicationLayer CompositeConfig   applicationLayer,
            @RemoteLayer      CompositeConfig   remoteLayer,
            @RuntimeLayer     SettableConfig    runtimeLayer,
            @DefaultsLayer    SettableConfig    defaultsLayer,
                              ConfigLoader      loader
            ) throws Exception {
        
        // First load the single application configuration 
        LinkedHashMap<String, Config> loadedConfigs = loader
                .newLoader()
                .withCascadeStrategy(NoCascadeStrategy.INSTANCE)
                .load(archaiusConfiguration.getConfigName());
        if (loadedConfigs != null) {
            applicationLayer.addConfigs(loadedConfigs);
        }

        // Load any defaults from code.  These defaults may be initialized as part of 
        // conditional module loading provided by Governator.  
        for (ConfigSeeder provider : archaiusConfiguration.getDefaultsLayerSeeders()) {
            defaultsLayer.setProperties(provider.get(config));
        }
 
        // load any runtime overrides
        for (ConfigSeeder provider : archaiusConfiguration.getRuntimeLayerSeeders()) {
            runtimeLayer.setProperties(provider.get(config));
        }
 
        // Finally, load any cascaded configuration files for the application
        loadedConfigs = loader.newLoader().withCascadeStrategy(archaiusConfiguration.getCascadeStrategy()).load(archaiusConfiguration.getConfigName());
        if (loadedConfigs != null) { 
            loadedConfigs.remove(archaiusConfiguration.getConfigName());
            for (Entry<String, Config> entry : loadedConfigs.entrySet()) {
                try {
                    applicationLayer.addConfig(entry.getKey(), entry.getValue());
                }
                catch (ConfigAlreadyExistsException e) {
                    // OK to ignore
                }
            }
        }
        
        // Remote layers most likely need some configuration so we load them after application
        // configuration has been loaded. 
        for (ConfigSeeder provider : archaiusConfiguration.getRemoteLayerSeeders()) {
            remoteLayer.addConfig("remote" + idCounter.incrementAndGet(), provider.get(config));
        }
        
        for (ConfigListener listener : archaiusConfiguration.getConfigListeners()) {
            config.addListener(listener);
        }
        
        return config;
    }
    
    /**
     * Top level 'fast' property factory which is bound to the root configuration 
     * @param root
     * @return
     */
    @Provides
    @Singleton
    PropertyFactory getPropertyFactory(Config root) {
        return DefaultPropertyFactory.from(root);
    }

    @Provides
    @Singleton
    ConfigProxyFactory getProxyFactory(ArchaiusConfiguration config, PropertyFactory factory) {
        return new ConfigProxyFactory(config.getDecoder(), factory);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    
    @Override
    public boolean equals(Object obj) {
        return ArchaiusModule.class.equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return ArchaiusModule.class.hashCode();
    }

}
