package netflix.archaius;

import java.util.ArrayList;
import java.util.List;

import netflix.archaius.cascade.SimpleCascadeStrategy;
import netflix.archaius.config.CompositeConfig;
import netflix.archaius.interpolate.CommonsStrInterpolatorFactory;
import netflix.archaius.property.DefaultObservablePropertyFactory;

/**
 * Core Archaius configuration manager API.  
 * 
 * <h1>Initialization</h1>
 * The {@link ConfigManager} is constructed using a builder through which it can be customized.
 * 
 * <h1>Top level Config API</h1>
 * The {@link ConfigManager} is a CompositeConfig and as such serves as the top level container 
 * for retrieving configurations.  Configurations are prioritized in the order in which they are
 * added so that a property defined in an earlier Config takes precedence over a Config added
 * later.
 * 
 * For example, in the following configManager setup properties will be resolved in the order
 * myDynamicConfig, SystemConfig, Environment where the first Config to contain the property 
 * will be used.
 * 
 * {@code
 * <pre>
 *    configManager.addConfig(myDynamicConfig)
 *                 .addConfig(new SystemConfig())
 *                 .addConfig(new EnvironmentConfig);
 * </pre>
 * }
 * 
 * <h1>Dynamic configuration</h1>
 * 
 * In addition to static configurations ConfigManager exposes an API to fetch {@link ObservableProperty} 
 * objects through which client code can receive update notification for properties.  Note that 
 * updates to ObservableProperty are pushed once an underlying DynamicConfig configuration 
 * changes.  Multiple DynamicConfig's may be added to the ConfigManager and all will be automatically
 * subscribed to for configuration changes.
 * 
 * <h1>Config Loader API</h1>
 * 
 * While Config objects may be added manually by calling addConfig, ConfigManager also exposes a 
 * convenient API for more feature rich loading which includes resource cascade naming. 
 * 
 * <h1>Config types</h1>
 * 
 * Archaius supports various configuration types such as, SystemConfig, EnvironmentConfig, DyanmicConfig,
 * and configurations loaded via the Loader api.
 * 
 * @author elandau
 *
 */
public class ConfigManager extends CompositeConfig {
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();
    private static final String NAME = "ROOT";
    
    public static interface Loader {
        Loader withCascadeStrategy(CascadeStrategy strategy);
        Loader withName(String name);
        Loader withClassLoader(ClassLoader loader);
        Loader withFailOnFirst(boolean flag);
        Config load(String url);
    }

    public static class Builder {
        private StrInterpolatorFactory    interpolator;
        private ObservablePropertyFactory propertyFactory;
        private final List<ConfigLoader>  loaders = new ArrayList<ConfigLoader>();
        private CascadeStrategy           defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean                   failOnFirst = true;
        
        public Builder withStrInterpolator(StrInterpolatorFactory interpolator) {
            this.interpolator = interpolator;
            return this;
        }
        
        public Builder withObservablePropertyFactory(ObservablePropertyFactory factory) {
            this.propertyFactory = factory;
            return this;
        }
        
        public Builder withConfigLoader(ConfigLoader loader) {
            this.loaders.add(loader);
            return this;
        }
        
        public Builder withDefaultCascadingStrategy(CascadeStrategy strategy) {
            this.defaultStrategy = strategy;
            return this;
        }

        public Builder withFailOnFirst(boolean flag) {
            this.failOnFirst = flag;
            return this;
        }
        
        public ConfigManager build() {
            if (this.interpolator == null) {
                this.interpolator = new CommonsStrInterpolatorFactory();
            }
            return new ConfigManager(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final ObservablePropertyRegistry registery;
    private final StrInterpolator interpolator;
    private final List<ConfigLoader> loaders;
    private final CascadeStrategy    defaultStrategy;

    public ConfigManager(Builder builder) {
        super(NAME);
        
        this.interpolator    = builder.interpolator.create(this);
        this.loaders         = builder.loaders;
        this.defaultStrategy = builder.defaultStrategy;
        
        this.setStrInterpolator(interpolator);

        ObservablePropertyFactory propertyFactory = builder.propertyFactory;
        if (propertyFactory == null) {
            propertyFactory = new DefaultObservablePropertyFactory(this);
        }

        this.registery = new ObservablePropertyRegistry(propertyFactory);
    }
    
    public ObservableProperty observe(String key) {
        return this.registery.get(key);
    }
    
    public Loader newLoader() {
        return new Loader() {
            private CascadeStrategy strategy = defaultStrategy;
            private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            private String name;
            private boolean failOnFirst = true;
            
            @Override
            public Loader withCascadeStrategy(CascadeStrategy strategy) {
                this.strategy = strategy;
                return this;
            }
            
            @Override
            public Loader withClassLoader(ClassLoader classLoader) {
                this.classLoader = classLoader;
                return this;
            }
            
            @Override
            public Loader withName(String name) {
                this.name = name;
                return this;
            }
            
            @Override
            public Loader withFailOnFirst(boolean flag) {
                this.failOnFirst = flag;
                return this;
            }

            @Override
            public Config load(String basename) {
                List<Config> configs = new ArrayList<Config>();
                for (String permname : strategy.generate(basename, interpolator)) {
                    for (ConfigLoader loader : loaders) {
                        if (loader.canLoad(name)) {
                            Config config = loader.load(permname);
                            if (config != null) {
                                configs.add(config);
                            }
                        }
                    }
                }
                return configs.isEmpty() 
                        ? null                                   // none 
                        : configs.size() == 1
                           ? configs.get(0)                      // one 
                           : new CompositeConfig(name, configs); // multiple 
            }
        };
    }    
    @Override
    public CompositeConfig addConfig(Config config) {
        if (config == null) {
            return this;
        }
        
        // Special registration for DynamicConfig
        if (DynamicConfig.class.isAssignableFrom(config.getClass())) {
            ((DynamicConfig)config).addListener(registery);
        }
        return super.addConfig(config);
    }
}
