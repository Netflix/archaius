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
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.inject.ApplicationLayer;
import com.netflix.archaius.inject.EnvironmentLayer;
import com.netflix.archaius.inject.LibrariesLayer;
import com.netflix.archaius.inject.OverrideLayer;
import com.netflix.archaius.inject.RuntimeLayer;
import com.netflix.archaius.inject.SystemLayer;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.loaders.PropertiesConfigReader;

/**
 * Guice module with default bindings to enable Config injection and 
 * configuration mapping/binding in a Guice based application.
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
    private static final String OVERRIDE_LAYER_NAME    = "OVERRIDE";
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
        
        binder().disableCircularProxies();
    }
    
    @Provides
    @Singleton
    @ApplicationLayer
    final String getApplicationName() {
        return DEFAULT_CONFIG_NAME;
    }
    
    public static class RuntimeLayerOptional {
        @com.google.inject.Inject(optional=true)
        @RuntimeLayer
        Properties properties;
    }
    
    @Provides
    @Singleton
    @RuntimeLayer
    final SettableConfig getSettableConfig(RuntimeLayerOptional optional) {
        DefaultSettableConfig config = new DefaultSettableConfig();
        if (optional.properties != null)
            config.setProperties(optional.properties);
        return config;
    }
    
    @Provides
    @Singleton
    @ApplicationLayer
    final CascadeStrategy getApplicationCascadeStrategy() {
        return new NoCascadeStrategy();
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
    final Config getApplicationLayer(
            @SystemLayer      Config            systemConfig, 
            @EnvironmentLayer Config            envConfig, 
            Set<ConfigReader>                   readers, 
            @ApplicationLayer CascadeStrategy   strategy,
            @ApplicationLayer String            appName) throws ConfigException {
        
        CompositeConfig config = new CompositeConfig();
        config.addConfig(SYSTEM_LAYER_NAME, systemConfig);
        config.addConfig(ENVIRONMENT_LAYER_NAME, envConfig);
        
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withStrLookup(ConfigStrLookup.from(config))
                .withConfigReader(readers)
                .withDefaultCascadingStrategy(strategy)
                .withFailOnFirst(false)
                .build();

        return loader.newLoader().load(appName);
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
            Config              config,
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
    @SystemLayer
    final Config getSystemLayer() {
        return new SystemConfig();
    }
    
    @Provides
    @Singleton
    @OverrideLayer
    final CompositeConfig getOverrideLayer() {
        return new CompositeConfig();
    }
    
    @Provides
    @Singleton
    @OverrideLayer
    final Config getOverrideLayer(@OverrideLayer CompositeConfig config) {
        return config;
    }
    
    @Provides
    @Singleton
    @EnvironmentLayer
    final Config getEnvironmentLayer() {
        return new EnvironmentConfig();
    }
    
    @Provides
    @Singleton
    final CascadeStrategy getCascadeStrategy(@ApplicationLayer CascadeStrategy strategy) {
        return strategy;
    }
    
    @Provides
    @Singleton
    final Decoder getDecoder() {
        return DefaultDecoder.INSTANCE;
    }

    /**
     * Override the binding to Config.class to specify a different override hierarchy
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
            @RuntimeLayer     SettableConfig     settableLayer,
            @OverrideLayer    Config             overrideLayer,
            @SystemLayer      Config             systemLayer,
            @EnvironmentLayer Config             environmentLayer,
            @ApplicationLayer Config             applicationLayer, 
            @LibrariesLayer   CompositeConfig    librariesLayer
            ) throws ConfigException {
        
        CompositeConfig root = new CompositeConfig();
        root.addConfig(RUNTIME_LAYER_NAME, settableLayer);
        
        if (!(overrideLayer instanceof EmptyConfig)) {
            root.addConfig(OVERRIDE_LAYER_NAME, overrideLayer);
        }
        
        if (!(systemLayer instanceof EmptyConfig)) {
            root.addConfig(SYSTEM_LAYER_NAME, systemLayer);
        }
        
        if (!(environmentLayer instanceof EmptyConfig)) {
            root.addConfig(ENVIRONMENT_LAYER_NAME, environmentLayer);
        }
        
        if (!(applicationLayer instanceof EmptyConfig)) {
            root.addConfig(APPLICATION_LAYER_NAME, applicationLayer);
        }
        
        root.addConfig(LIBRARIES_LAYER_NAME, librariesLayer);
        
        return root;
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
