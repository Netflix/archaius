package netflix.archaius;

import netflix.archaius.config.CompositeConfig;
import netflix.archaius.interpolate.CommonsStrInterpolatorFactory;
import netflix.archaius.property.DefaultObservablePropertyFactory;

/**
 * Root Archaius config.  All properties should be accessed via this Config..  
 * 
 * <h1>Initialization</h1>
 * The {@link RootConfig} is constructed using a builder through which it can be customized.
 * 
 * <h1>Top level Config API</h1>
 * The {@link RootConfig} is a CompositeConfig and as such serves as the top level container 
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
 * @author elandau
 *
 */
public class RootConfig extends CompositeConfig {
    private static final String NAME = "ROOT";
    
    public static class Builder {
        private StrInterpolatorFactory    interpolator;
        private ObservablePropertyFactory propertyFactory;
        
        public Builder withStrInterpolator(StrInterpolatorFactory interpolator) {
            this.interpolator = interpolator;
            return this;
        }
        
        public Builder withObservablePropertyFactory(ObservablePropertyFactory factory) {
            this.propertyFactory = factory;
            return this;
        }
        
        public RootConfig build() {
            if (this.interpolator == null) {
                this.interpolator = new CommonsStrInterpolatorFactory();
            }
            return new RootConfig(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final ObservablePropertyRegistry registery;

    public RootConfig(Builder builder) {
        super(NAME);
        
        this.setStrInterpolator(builder.interpolator.create(this));

        ObservablePropertyFactory propertyFactory = builder.propertyFactory;
        if (propertyFactory == null) {
            propertyFactory = new DefaultObservablePropertyFactory(this);
        }

        this.registery = new ObservablePropertyRegistry(propertyFactory);
    }
    
    public ObservableProperty observe(String key) {
        return this.registery.get(key);
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
