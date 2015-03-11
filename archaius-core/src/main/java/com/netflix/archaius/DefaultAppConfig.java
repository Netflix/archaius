package com.netflix.archaius;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.cascade.SimpleCascadeStrategy;
import com.netflix.archaius.config.CascadingCompositeConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SimpleDynamicConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.interpolate.CommonsStrInterpolatorFactory;
import com.netflix.archaius.loaders.PropertiesConfigReader;
import com.netflix.archaius.property.DefaultPropertyContainer;
import com.netflix.archaius.property.PropertyFactoryDynamicConfigObserver;

/**
 * Main AppConfig to be used as the top level entry point for application configuration.
 * This implementation is provided as a best practices approach to dealing with 
 * application configuration by extending composite configuration for a specific override
 * structure.  
 * 
 * <h1>Initialization</h1>
 * The {@link DefaultAppConfig} is constructed using a builder through which it can be customized.
 * 
 * <h1>Top level Config API</h1>
 * The {@link DefaultAppConfig} is a CompositeConfig and as such serves as the top level container 
 * for retrieving configurations.  Configurations follow a specific override structure,
 * 
 * RUNTIME      - Properties set via code have absolute priority
 * DYNAMIC      - Properties loaded from a remote override service.  DynamicConfig derived
 *                objects are added to this layer by calling {@link DefaultAppConfig#addConfigXXX()}
 * APPLICATION  - Properties loaded at startup from 'config.properties' and variants
 * LIBRARY      - Properties loaded by libraries or subsystems of the application.
 *                Calling {@link DefaultAppConfig#addConfigXXX()} loads Configs into this layer.
 * SYSTEM       - System.getProperties()
 * ENVIRONMENT  - System.getenv()
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
public class DefaultAppConfig extends CascadingCompositeConfig implements AppConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAppConfig.class);
    
    private static final String DEFAULT_APP_CONFIG_NAME = "config";
    
    private static final String NAME              = "ROOT";
    private static final String OVERRIDE_LAYER    = "OVERRIDE";
    private static final String DYNAMIC_LAYER     = "DYNAMIC";
    private static final String APPLICATION_LAYER = "APPLICATION";
    private static final String LIBRARY_LAYER     = "LIBRARY";
    
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();
    
    public static class Builder {
        private StrInterpolatorFactory    interpolator;
        private final List<ConfigReader>  loaders              = new ArrayList<ConfigReader>();
        private CascadeStrategy           defaultStrategy      = DEFAULT_CASCADE_STRATEGY;
        private boolean                   failOnFirst          = true;
        private boolean                   includeSystem        = true;
        private boolean                   includeEnvironment   = true;
        private boolean                   includeDynamicConfig = true;
        private String                    configName           = DEFAULT_APP_CONFIG_NAME;
        private Properties                props;
        private Decoder                   decoder              = new DefaultDecoder();

        public Builder withStrInterpolator(StrInterpolatorFactory interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        /**
         * Call to include or exclude SystemConfig.  Default is true.
         */
        public Builder withSystemConfig(boolean flag) {
            this.includeSystem = flag;
            return this;
        }
        
        /**
         * Call to include or exclude EnvironmentConfig.  Default is true.
         */
        public Builder withEnvironmentConfig(boolean flag) {
            this.includeEnvironment = flag;
            return this;
        }
        
        /**
         * Call to include or exclude DynamicConfig. Default is true.
         */
        public Builder withDynamicConfig(boolean flag) {
            this.includeDynamicConfig = flag;
            return this;
        }
        
        /**
         * Can be called multiple times to add multiple ConfigLoader to be used when 
         * loading application and library properties.  If no loaders are added AppConfig
         * will use PropertiesConfigLoader.
         */
        public Builder withConfigLoader(ConfigReader loader) {
            this.loaders.add(loader);
            return this;
        }

        /**
         * Default cascade strategy to use for loading application and library properties.
         * Library cascade strategies may be configured on the loader returned by newLoader.
         */
        public Builder withDefaultCascadingStrategy(CascadeStrategy strategy) {
            this.defaultStrategy = strategy;
            return this;
        }

        /**
         * Enable/disable failure if the first file in a cascade list of properties fails 
         * to load.
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
            this.configName = name;
            return this;
        }

        /**
         * Decoder used to decode properties to arbitrary types.
         */
        public Builder withDecoder(Decoder decoder) {
            this.decoder = decoder;
            return this;
        }
        
        public DefaultAppConfig build() {
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
    
    public static DefaultAppConfig createDefault() {
        return new Builder().build();
    }
    
    private final SimpleDynamicConfig          runtime;
    private final CascadingCompositeConfig     dynamic;
    private final CascadingCompositeConfig     library;
    private final PropertyFactoryDynamicConfigObserver dynamicObserver;
    private final DefaultConfigLoader          loader;
    
    public DefaultAppConfig(Builder builder) {
        super(NAME);
        
        try {
            // The following are added first, before application configuration
            // to allow for replacements in the application configuration cascade
            // loading.
            super.addConfigLast(runtime = new SimpleDynamicConfig(OVERRIDE_LAYER));
            runtime.setProperties(builder.props);
            
            if (builder.includeDynamicConfig) {
                super.addConfigLast(dynamic = new CascadingCompositeConfig(DYNAMIC_LAYER));
            }
            else {
                dynamic = null;
            }
            
            if (builder.includeSystem) {
                super.addConfigLast(new SystemConfig());
            }
            
            if (builder.includeEnvironment) {
                super.addConfigLast(new EnvironmentConfig());
            }

            if (builder.decoder != null) {
                super.setDecoder(builder.decoder);
            }

            loader = DefaultConfigLoader.builder()
                    .withConfigReader(builder.loaders)
                    .withDefaultCascadingStrategy(builder.defaultStrategy)
                    .withFailOnFirst(builder.failOnFirst)
                    .withStrInterpolator(getStrInterpolator())
                    .build();
                
            super.addConfigLast(loader.newLoader().withName(APPLICATION_LAYER).load(builder.configName));
            super.addConfigLast(library = new CascadingCompositeConfig(LIBRARY_LAYER));
            
            this.setStrInterpolator(builder.interpolator.create(this));
            
            this.dynamicObserver = new PropertyFactoryDynamicConfigObserver(new PropertyFactory() {
                @Override
                public PropertyContainer connectProperty(String propName) {
                    return new DefaultPropertyContainer(propName, DefaultAppConfig.this);
                }
            });
            
            library.addListener(new Listener() {
                @Override
                public void onConfigAdded(Config child) {
                    dynamicObserver.invalidate();
                }                
            });
            if (dynamic != null) {
                dynamic.addListener(new Listener() {
                    @Override
                    public void onConfigAdded(Config child) {
                        dynamicObserver.invalidate();
                    }
                });
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addConfigFirst(Config child) throws ConfigException {
        LOG.info("Adding configuration : " + child.getName());
        if (child instanceof DynamicConfig) {
            DynamicConfig dynamicChild = (DynamicConfig)child;
            dynamicChild.addListener(dynamicObserver);
            this.dynamic.addConfigFirst(child);
        } 
        else {
            this.library.addConfigFirst(child);
        }
    }
    
    public void addConfigLast(Config child) throws ConfigException {
        LOG.info("Adding configuration : " + child.getName());
        if (child instanceof DynamicConfig) {
            DynamicConfig dynamicChild = (DynamicConfig)child;
            dynamicChild.addListener(dynamicObserver);
            this.dynamic.addConfigLast(child);
        }
        else {
            this.library.addConfigLast(child);
        }
    }
    
    public Loader newLoader() {
        return loader.newLoader();
    }
    
    @Override
    public PropertyContainer connectProperty(String propName) {
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
}
