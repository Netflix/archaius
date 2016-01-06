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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
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
 */
public final class ArchaiusModule extends AbstractModule {
    private static final String DEFAULT_CONFIG_NAME     = "application";
    
    private static final String RUNTIME_LAYER_NAME      = "RUNTIME";
    private static final String REMOTE_LAYER_NAME       = "REMOTE";
    private static final String SYSTEM_LAYER_NAME       = "SYSTEM";
    private static final String ENVIRONMENT_LAYER_NAME  = "ENVIRONMENT";
    private static final String APPLICATION_LAYER_NAME  = "APPLICATION";
    private static final String LIBRARIES_LAYER_NAME    = "LIBRARIES";
    
    // These are here to force using the backwards compatibility bridge for 
    // Archaius1's static API
    static {
        System.setProperty("archaius.default.configuration.class",      "com.netflix.archaius.bridge.StaticAbstractConfiguration");
        System.setProperty("archaius.default.deploymentContext.class",  "com.netflix.archaius.bridge.StaticDeploymentContext");
    }
    
    private String configName = DEFAULT_CONFIG_NAME;
    private Class<? extends CascadeStrategy> cascadeStrategy = NoCascadeStrategy.class;

    private final SettableConfig  runtimeLayer;
    private final CompositeConfig remoteLayer;
    private final CompositeConfig applicationLayer;
    private final CompositeConfig librariesLayer;
    private final CompositeConfig rawConfig;
    private int uniqueNameCounter = 0;

    public ArchaiusModule() {
        this.runtimeLayer     = new DefaultSettableConfig();
        this.applicationLayer = new DefaultCompositeConfig();
        this.librariesLayer   = new DefaultCompositeConfig();
        this.remoteLayer      = new DefaultCompositeConfig();
        try {
            this.rawConfig    = DefaultCompositeConfig.builder()
                    .withConfig(RUNTIME_LAYER_NAME,      runtimeLayer)
                    .withConfig(REMOTE_LAYER_NAME,       remoteLayer)
                    .withConfig(SYSTEM_LAYER_NAME,       SystemConfig.INSTANCE)
                    .withConfig(ENVIRONMENT_LAYER_NAME,  EnvironmentConfig.INSTANCE)
                    .withConfig(APPLICATION_LAYER_NAME,  applicationLayer)
                    .withConfig(LIBRARIES_LAYER_NAME,    librariesLayer)
                    .build();
        } 
        catch (ConfigException e) {
            throw new ProvisionException("Error creating raw configuration", e);
        }
    }
    
    public ArchaiusModule withConfigName(String value) {
        this.configName = value;
        return this;
    }
    
    public ArchaiusModule withApplicationOverrides(Properties prop) {
        return withApplicationOverrides(MapConfig.from(prop));
    }
    
    public ArchaiusModule withApplicationOverrides(Config config) {
        try {
            uniqueNameCounter++;
            applicationLayer.addConfig("override-" + uniqueNameCounter, config);
        } 
        catch (ConfigException e) {
            throw new ProvisionException("Failed to add application overrides" , e);
        }
        return this;
    }
    
    public ArchaiusModule withCascadeStrategy(Class<? extends CascadeStrategy> cascadeStrategy) {
        this.cascadeStrategy = cascadeStrategy;
        return this;
    }
    
    @Override
    protected void configure() {
        bindListener(Matchers.any(), new ConfigurationInjectingListener());
        
        bind(CascadeStrategy.class).to(cascadeStrategy);
        
        Multibinder.newSetBinder(binder(), ConfigReader.class)
            .addBinding().to(PropertiesConfigReader.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    @RuntimeLayer
    SettableConfig getSettableConfig() {
        return runtimeLayer;
    }
    
    @Provides
    @Singleton
    @LibrariesLayer
    CompositeConfig getLibrariesLayer() {
        return librariesLayer;
    }
    
    @Provides
    @Singleton
    @Raw
    Config getRawConfig() {
        return rawConfig;
    }
    
    @Singleton
    private static class OptionalRemoteLayer {
        @Inject(optional=true)
        @RemoteLayer
        Config remoteLayer;
    }
    
    @Provides
    @Singleton
    Config getConfig(ConfigLoader loader, OptionalRemoteLayer optionalRemoteLayer) throws Exception {
        this.applicationLayer.addConfig(configName,  loader
            .newLoader()
            .load(configName));

        // load any runtime overrides
        if (null != optionalRemoteLayer.remoteLayer) {
            // TODO: Ideally this should replace the remoteLayer in config but there is a bug in archaius
            //       where the replaced layer moves to the end of the hierarchy
            remoteLayer.addConfig("remote", optionalRemoteLayer.remoteLayer);
        }
        
        return rawConfig;
    }
        
    @Provides
    @Singleton
    ConfigLoader getLoader(
            @LibrariesLayer       CompositeConfig libraries,
            CascadeStrategy       cascadingStrategy,
            Set<ConfigReader>     readers
            ) throws ConfigException {
        
        return DefaultConfigLoader.builder()
            .withConfigReaders(readers)
            .withDefaultCascadingStrategy(cascadingStrategy)
            .withStrLookup(ConfigStrLookup.from(rawConfig))
            .build();
    }
    
    @Provides
    @Singleton
    public Decoder getDecoder() {
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
