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
package com.netflix.archaius;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.cascade.SimpleCascadeStrategy;
import com.netflix.archaius.config.CascadingCompositeConfig;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.DelegatingConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SimpleDynamicConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.interpolate.CommonsStrInterpolatorFactory;
import com.netflix.archaius.loaders.PropertiesConfigReader;
import com.netflix.archaius.property.DefaultPropertyContainer;
import com.netflix.archaius.property.PropertyFactoryDynamicConfigListener;

/**
 * Main AppConfig to be used as the top level entry point for application configuration.
 * This implementation is provided as a best practices approach to dealing with 
 * application configuration by extending composite configuration for a specific override
 * structure.  
 * 
 * <h1>Override structure</h1>
 * The {@link DefaultAppConfig} is a CompositeConfig and as such serves as the top level container 
 * for retrieving configurations.  Configurations follow a specific override structure,
 * 
 * RUNTIME      - Properties set via code have absolute priority
 * OVERRIDE     - Properties loaded from a remote override service.  DynamicConfig derived
 *                objects are added to this layer by calling {@link DefaultAppConfig#addConfigXXX()}
 * SYSTEM       - System.getProperties()
 * ENVIRONMENT  - System.getenv()
 * APPLICATION  - Properties loaded at startup from 'config.properties' and variants
 * LIBRARY      - Properties loaded by libraries or subsystems of the application.
 *                Calling {@link DefaultAppConfig#addConfigXXX()} loads Configs into this layer.
 * 
 * <h1>Dynamic configuration</h1>
 * 
 * In addition to static configurations AppConfig exposes an API to fetch {@link PropertyDsl} 
 * objects through which client code can receive update notification for properties.  Note that 
 * updates to ObservableProperty are pushed once an underlying DynamicConfig configuration 
 * changes.  Multiple DynamicConfig's may be added to the ConfigManager and all will be automatically
 * subscribed to for configuration changes.
 * 
 * @author elandau
 *
 */
public class DefaultAppConfig extends DelegatingConfig implements AppConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAppConfig.class);
    
    public static final String                DEFAULT_APP_CONFIG_NAME = "config";
    public static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();
    
    public static final String NAME              = "APP_CONFIG";
    public static final String ROOT              = "ROOT";
    public static final String OVERRIDE_LAYER    = "OVERRIDE";
    public static final String DYNAMIC_LAYER     = "DYNAMIC";
    public static final String APPLICATION_LAYER = "APPLICATION";
    public static final String LIBRARY_LAYER     = "LIBRARY";
    
    public static class Builder {
        private StrInterpolatorFactory    interpolator;
        private final List<ConfigReader>  loaders                = new ArrayList<ConfigReader>();
        private CascadeStrategy           defaultStrategy        = DEFAULT_CASCADE_STRATEGY;
        private boolean                   failOnFirst            = true;
        private String                    configName             = DEFAULT_APP_CONFIG_NAME;
        private Properties                props;
        private Decoder                   decoder;
        private List<Config>              overrideConfigs        = new ArrayList<Config>();

        public Builder withStrInterpolator(StrInterpolatorFactory interpolator) {
            if (interpolator == null) {
                throw new IllegalArgumentException("StrInterpolatorFactory cannot be null or empty");
            }

            this.interpolator = interpolator;
            return this;
        }

        /**
         * Can be called multiple times to add multiple ConfigLoader to be used when 
         * loading application and library properties.  If no loaders are added AppConfig
         * will use PropertiesConfigLoader.
         */
        public Builder withConfigReader(ConfigReader loader) {
            if (loader != null) {   
                this.loaders.add(loader);
            }
            return this;
        }

        public Builder withOverrideConfig(Config config) {
            this.overrideConfigs.add(config);
            return this;
        }
        
        /**
         * Default cascade strategy to use for loading application and library properties.
         * Library cascade strategies may be configured on the loader returned by newLoader.
         */
        public Builder withDefaultCascadingStrategy(CascadeStrategy strategy) {
            this.defaultStrategy = strategy == null 
                                 ? DEFAULT_CASCADE_STRATEGY 
                                 : strategy;
            return this;
        }

        /**
         * Enable/disable failure if the first file in a cascade list of properties fails 
         * to load.  This includes the main application config file.
         */
        public Builder withFailOnFirst(boolean flag) {
            this.failOnFirst = flag;
            return this;
        }
        
        /**
         * Properties to load into the runtime layer at startup.
         */
        public Builder withProperties(Properties props) {
            this.props = props;
            return this;
        }

        /**
         * Name of application configuration to load at startup.  Default is 'config'.
         */
        public Builder withApplicationConfigName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            
            this.configName = name;
            return this;
        }

        /**
         * Decoder used to decode properties to arbitrary types.
         */
        public Builder withDecoder(Decoder decoder) {
            if (decoder == null) {
                throw new IllegalArgumentException("Decoder cannot be null");
            }
            this.decoder = decoder;
            return this;
        }
        
        public DefaultAppConfig build() throws ConfigException {
            if (this.interpolator == null) {
                this.interpolator = new CommonsStrInterpolatorFactory();
            }
            if (this.loaders.isEmpty()) {
                this.loaders.add(new PropertiesConfigReader());
            }
            return new DefaultAppConfig(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static DefaultAppConfig createDefault() throws ConfigException {
        return new Builder().build();
    }
    
    private final CascadingCompositeConfig     root;
    private final SimpleDynamicConfig          runtime;
    private final CascadingCompositeConfig     dynamic;
    private final CascadingCompositeConfig     library;
    private final DefaultConfigLoader          loader;
    private final PropertyFactoryDynamicConfigListener dynamicObserver;
    
    public DefaultAppConfig(Builder builder) throws ConfigException {
        super("APP_CONFIG");
        
        try {
            this.root = new CascadingCompositeConfig(NAME);
            
            if (builder.decoder != null) {
                setDecoder(builder.decoder);
                this.root.setDecoder(builder.decoder);
            }

            this.setStrInterpolator(builder.interpolator.create(this));
            this.root.setStrInterpolator(this.getStrInterpolator());

            // The following are added first, before application configuration
            // to allow for replacements in the application configuration cascade
            // loading.
            root.addConfig(runtime = new SimpleDynamicConfig(OVERRIDE_LAYER));
            runtime.setProperties(builder.props);
            
            root.addConfig(dynamic = new CascadingCompositeConfig(DYNAMIC_LAYER));
            root.addConfig(new SystemConfig());
            root.addConfig(new EnvironmentConfig());
            
            loader = DefaultConfigLoader.builder()
                    .withConfigReader(builder.loaders)
                    .withDefaultCascadingStrategy(builder.defaultStrategy)
                    .withFailOnFirst(builder.failOnFirst)
                    .withStrInterpolator(getStrInterpolator())
                    .build();
                
            root.addConfig(loader.newLoader().withName(APPLICATION_LAYER).load(builder.configName));
            root.addConfig(library = new CascadingCompositeConfig(LIBRARY_LAYER));
            
            this.dynamicObserver = new PropertyFactoryDynamicConfigListener(new PropertyFactory() {
                @Override
                public PropertyContainer getProperty(String propName) {
                    return new DefaultPropertyContainer(propName, DefaultAppConfig.this);
                }
            });
            
            library.addListener(dynamicObserver);
            dynamic.addListener(dynamicObserver);
            dynamic.addConfigs(builder.overrideConfigs);
        }
        catch (ConfigException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ConfigException("Unknown error", e);
        }
    }
    
    public Loader newLoader() {
        return loader.newLoader();
    }
    
    @Override
    public PropertyContainer getProperty(String propName) {
        return dynamicObserver.create(propName);
    }

    @Override
    public void setProperty(String propName, Object propValue) {
        PropertyContainer prop = dynamicObserver.get(propName);
        runtime.setProperty(propName, propValue);
        if (prop != null) {
            prop.update();
        }
    }

    @Override
    public void clearProperty(String propName) {
        PropertyContainer prop = dynamicObserver.get(propName);
        runtime.clearProperty(propName);
        if (prop != null) {
            prop.update();
        }
    }

    @Override
    public void setProperties(Properties properties) {
        runtime.setProperties(properties);
        dynamicObserver.invalidate();
    }

    @Override
    public Config getLayer(String name) {
        return root.getConfig(name);
    }

    @Override
    public CompositeConfig getCompositeLayer(String name) throws ConfigException {
        Config layer = root.getConfig(name);
        if (layer != null) {
            if (layer instanceof CompositeConfig) {
                return (CompositeConfig)layer;
            }
            else {
                throw new ConfigException(String.format("Layer '%s' is not a CompositeConfig", name));
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        return root.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return root.isEmpty();
    }

    @Override
    public Iterator<String> getKeys() {
        return root.getKeys();
    }

    @Override
    protected Config getConfigWithProperty(String key, boolean failOnNotFound) {
        return root;
    }

    @Override
    public Collection<String> getLayerNames() {
        return root.getConfigNames();
    }
}
