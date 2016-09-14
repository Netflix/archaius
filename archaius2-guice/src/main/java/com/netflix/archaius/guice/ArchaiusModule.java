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

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.inject.DefaultLayer;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.config.MapConfig;

/**
 * Guice Module for enabling archaius and making its components injectable.  Installing this
 * module also enables the following functionality.
 * 
 * <ul>
 * <li>Injectable Config</li>
 * <li>Configuration Proxy</li>
 * <li>Configuration mapping</li>
 * </uL>
 * 
 * This module creates an injectable Config instance that has the following override structure in
 * order of precedence. 
 * 
 *  RUNTIME     - properties set from code
 *  REMOTE      - properties loaded from a remote source
 *  SYSTEM      - System properties
 *  ENVIRONMENT - Environment properties
 *  APPLICATION - Configuration loaded by the application
 *  LIBRARIES   - Configuration loaded by libraries used by the application
 *  DEFAULT     - Default properties driven by bindings
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
 * public class FooRemoteModule extends ArchaiusModule {
 *     {@literal @}Provides
 *     {@literal @}RemoteLayer
 *     Config getRemoteConfig({@literal @}Raw Config config) {
 *         return new FooRemoteConfigImplementaiton(config);
 *     }
 * }
 * </code>
 */
public class ArchaiusModule extends AbstractModule {
    @Deprecated
    private Class<? extends CascadeStrategy> cascadeStrategy = null;

    @Deprecated
    private Config applicationOverride;
    
    @Deprecated
    private String configName;
    
    @Deprecated
    public ArchaiusModule withConfigName(String value) {
        this.configName = value;
        return this;
    }
    
    @Deprecated
    public ArchaiusModule withApplicationOverrides(Properties prop) {
        return withApplicationOverrides(MapConfig.from(prop));
    }
    
    @Deprecated
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
    
    protected void configureArchaius() {
    }
    
    /**
     * Customize the filename for the main application configuration.  The default filename is
     * 'application'.  
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindConfigurationName().toInstance("myconfig");
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<String> bindConfigurationName() {
        return bind(String.class).annotatedWith(Names.named(InternalArchaiusModule.CONFIG_NAME_KEY));
    }
    
    /**
     * Set application overrides.  This is normally done for unit tests.
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindApplicationConfigurationOverride().toInstance(MapConfig.builder()
     *          .put("some_property_to_override", "value")
     *          .build()
     *          );
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<Config> bindApplicationConfigurationOverride() {
        return bind(Config.class).annotatedWith(ApplicationOverride.class);
    }
    
    /**
     * Specify the Config to use for the remote layer. 
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindRemoteConfig().to(SomeRemoteConfigImpl.class);
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<Config> bindRemoteConfig() {
        return bind(Config.class).annotatedWith(RemoteLayer.class);
    }
    
    /**
     * Specify the CascadeStrategy used to load environment overrides for application and
     * library configurations.
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindCascadeStrategy().to(MyCascadeStrategy.class);
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<CascadeStrategy> bindCascadeStrategy() {
        return bind(CascadeStrategy.class);
    }
    
    /**
     * Add a config to the bottom of the Config hierarchy.  Use this when configuration is added
     * through code.  Can be called multiple times as ConfigReader is added to a multibinding.
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindDefaultConfig().to(MyDefaultConfig.class);
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<Config> bindDefaultConfig() {
        return Multibinder.newSetBinder(binder(), Config.class, DefaultLayer.class).addBinding();
    }

    /**
     * Add support for a new configuration format.  Can be called multiple times to add support for
     * multiple file format.
     * 
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindConfigReader().to(SomeConfigFormatReader.class);
     *    }
     * });
     * </code>
     * 
     * @return LinkedBindingBuilder to which the implementation is set
     */
    protected LinkedBindingBuilder<Config> bindConfigReader() {
        return Multibinder.newSetBinder(binder(), Config.class, DefaultLayer.class).addBinding();
    }

    /**
     * Set application overrides to a particular resource.  This is normally done for unit tests.
     *
     * <code>
     * install(new ArchaiusModule() {
     *    {@literal @}Override
     *    protected void configureArchaius() {
     *        bindApplicationConfigurationOverrideResource("laptop");
     *    }
     * });
     * </code>
     *
     * @return
     */
    protected void bindApplicationConfigurationOverrideResource(String overrideResource)  {
        Multibinder.newSetBinder(binder(), String.class, ApplicationOverrideResources.class).permitDuplicates().addBinding().toInstance(overrideResource);
    }

    @Override
    protected final void configure() {
        install(new InternalArchaiusModule());
      
        configureArchaius();

        // TODO: Remove in next release
        if (configName != null) {
            this.bindConfigurationName().toInstance(configName);
        }
        
        // TODO: Remove in next release
        if (cascadeStrategy != null) {
            this.bindCascadeStrategy().to(cascadeStrategy);
        }

        // TODO: Remove in next release
        if (applicationOverride != null) {
            this.bindApplicationConfigurationOverride().toInstance(applicationOverride);
        }
    }
}
