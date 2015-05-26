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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Providers;
import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigLoader;
import com.netflix.archaius.ConfigReader;
import com.netflix.archaius.Decoder;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.ProxyFactory;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.inject.ApplicationLayer;
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
 * To override the binding use Guice's Modules.override()
 * 
 * <pre>
 * ```java
 * Modules
 *     .override(new ArchaiusModule())
 *     .with(new AbstractModule() {
 *         @Override
 *         protected void configure() {
 *             bind(Config.class).to ...
 *         }
 *     })
 * ```
 * </pre>
 * 
 * @author elandau
 *
 */
public final class ArchaiusModule extends AbstractModule {
    
    private static final String DEFAULT_CONFIG_NAME    = "application";
    private static final String RUNTIME_LAYER_NAME     = "RUNTIME";
    private static final String REMOTE_LAYER_NAME      = "REMOTE";
    private static final String SYSTEM_LAYER_NAME      = "SYSTEM";
    private static final String ENVIRONMENT_LAYER_NAME = "ENVIRONMENT";
    private static final String APPLICATION_LAYER_NAME = "APPLICATION";
    private static final String LIBRARIES_LAYER_NAME   = "LIBRARIES";

    public static class ConfigProvider<T> implements Provider<T> {
        private Class<T> type;
        
        @Inject
        ProxyFactory proxy;
        
        @Inject
        PropertyFactory factory;
        
        public ConfigProvider(Class<T> type) {
            this.type = type;
        }

        @Override
        public T get() {
            return proxy.newProxy(type, factory);
        }
    }
    
    public static <T> AbstractModule forProxy(final Class<T> proxy) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Provider<T> provider = new ConfigProvider<T>(proxy);
                requestInjection(provider);
                bind(proxy).toProvider(Providers.guicify(provider));
            }
        };
    }
    
    @Override
    final protected void configure() {
        ConfigurationInjectingListener listener = new ConfigurationInjectingListener();
        requestInjection(listener);
        bindListener(Matchers.any(), listener);
        
        Multibinder.newSetBinder(binder(), ConfigReader.class)
            .addBinding().to(PropertiesConfigReader.class);
        
        Multibinder.newSetBinder(binder(), ConfigSeeder.class, RuntimeLayer.class);
        Multibinder.newSetBinder(binder(), ConfigSeeder.class, RemoteLayer.class);
        
        binder().disableCircularProxies();
    }
    
    @Provides
    @Singleton
    @ApplicationLayer
    final String getConfigName() {
        return DEFAULT_CONFIG_NAME;
    }
    
    @Provides
    @Singleton
    @RuntimeLayer
    final SettableConfig getSettableConfig() {
        return new DefaultSettableConfig();
    }
    
    /**
     * Default loader for the application configuration using replacements from 
     * system and environment configuration
     * 
     * @param systemConfig
     * @param envConfig
     * @param readers
     * @param strategy
     * @param appName
     * @return
     * @throws ConfigException
     */
    @Provides
    @Singleton
    @ApplicationLayer 
    Config getApplicationLayer(CompositeConfig config) {
        return config;
    }

    @Provides
    @Singleton
    @ApplicationLayer 
    CompositeConfig getApplicationLayer() {
        return new CompositeConfig();
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
    final ConfigLoader getLoader(
            @RootLayer          Config config,
            CascadeStrategy     defaultStrategy,
            Set<ConfigReader>   readers
            ) {
        return DefaultConfigLoader.builder()
                .withConfigReader(readers)
                .withDefaultCascadingStrategy(defaultStrategy)
                .withFailOnFirst(false)
                .withStrLookup(ConfigStrLookup.from(config))
                .build();
    }

    @Provides
    @Singleton
    @LibrariesLayer
    final CompositeConfig getLibrariesLayer() {
        return new CompositeConfig();
    }
    
    @Provides
    @Singleton
    @RemoteLayer
    final CompositeConfig getOverrideLayer() {
        return new CompositeConfig();
    }
    
    @Provides
    @Singleton
    @RemoteLayer
    final Config getOverrideLayer(@RemoteLayer CompositeConfig config) {
        return config;
    }
    
    @Provides
    @Singleton
    final CascadeStrategy getCascadeStrategy() {
        return new NoCascadeStrategy();
    }
    
    @Provides
    @Singleton
    final Decoder getDecoder() {
        return DefaultDecoder.INSTANCE;
    }

    @Provides
    @Singleton
    @RootLayer
    final Config getInternalConfig(
            @RuntimeLayer     SettableConfig     settableLayer,
            @RemoteLayer      Config             overrideLayer,
            @ApplicationLayer CompositeConfig    applicationLayer, 
            @LibrariesLayer   CompositeConfig    librariesLayer) throws ConfigException {
        return CompositeConfig.builder()
                .withConfig(RUNTIME_LAYER_NAME,     settableLayer)
                .withConfig(REMOTE_LAYER_NAME,    overrideLayer)
                .withConfig(SYSTEM_LAYER_NAME,      SystemConfig.INSTANCE)
                .withConfig(ENVIRONMENT_LAYER_NAME, EnvironmentConfig.INSTANCE)
                .withConfig(APPLICATION_LAYER_NAME, applicationLayer)
                .withConfig(LIBRARIES_LAYER_NAME,   librariesLayer)
                .build();
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
     * @param overrideLayer
     * @return
     * @throws ConfigException
     */
    @Provides
    @Singleton
    final Config getConfig(
            @RootLayer        Config            config,
            @ApplicationLayer CompositeConfig   applicationLayer,
            @ApplicationLayer String            appName,
            @RemoteLayer      CompositeConfig   overrideLayer,
            @RuntimeLayer     SettableConfig    runtimeLayer,
                              ConfigLoader      loader,
                              CascadeStrategy   cascadeStrategy,
            @RuntimeLayer     Set<ConfigSeeder> runtimeConfigResolvers,
            @RemoteLayer      Set<ConfigSeeder> remoteConfigResolvers
            ) throws Exception {
        
        // First load the application configuration 
        LinkedHashMap<String, Config> loadedConfigs = loader.newLoader().withCascadeStrategy(new NoCascadeStrategy()).load(appName);
        if (loadedConfigs != null) {
            applicationLayer.addConfigs(loadedConfigs);
        }
        
        // Next load any runtime overrides
        for (ConfigSeeder provider : runtimeConfigResolvers) {
            runtimeLayer.setProperties(provider.get(config));
        }
 
        // Finally, load the remote layer
        for (ConfigSeeder provider : remoteConfigResolvers) {
            overrideLayer.addConfig("remote", provider.get(config));
        }
        
        // Finally, load any cascaded configuration files for the
        // application
        loadedConfigs = loader.newLoader().withCascadeStrategy(cascadeStrategy).load(appName);
        if (loadedConfigs != null) { 
            applicationLayer.replaceConfigs(loadedConfigs);
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
    final PropertyFactory getPropertyFactory(final Config root) {
        return DefaultPropertyFactory.from(root);
    }
}
