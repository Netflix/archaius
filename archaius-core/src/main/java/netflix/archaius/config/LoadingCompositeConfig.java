package netflix.archaius.config;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import netflix.archaius.CascadeStrategy;
import netflix.archaius.Config;
import netflix.archaius.ConfigLoader;
import netflix.archaius.cascade.SimpleCascadeStrategy;
import netflix.archaius.exceptions.ConfigurationException;
import netflix.archaius.interpolate.CommonsStrInterpolatorFactory;

/**
 * Specialized CompositeConfig with mechanism to load static configurations using
 * cascading resource names.
 * 
 * 
 * @author elandau
 *
 */
public class LoadingCompositeConfig extends CompositeConfig {
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();

    /**
     * DSL for loading a configuration
     * 
     * @author elandau
     *
     */
    public static interface Loader {
        Loader withCascadeStrategy(CascadeStrategy strategy);
        Loader withName(String name);
        Loader withClassLoader(ClassLoader loader);
        Loader withFailOnFirst(boolean flag);
        Config load(String resourceName);
    }

    public static class Builder {
        private final List<ConfigLoader>  loaders = new ArrayList<ConfigLoader>();
        private CascadeStrategy           defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean                   failOnFirst = true;
        private String                    name = "";
        
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
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public LoadingCompositeConfig build() {
            return new LoadingCompositeConfig(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final List<ConfigLoader> loaders;
    private final CascadeStrategy    defaultStrategy;
    
    public LoadingCompositeConfig(Builder builder) {
        super(builder.name);
        
        this.loaders         = builder.loaders;
        this.defaultStrategy = builder.defaultStrategy;
        this.setStrInterpolator(new CommonsStrInterpolatorFactory().create(this));
    }

    public void load(String name, URL url) throws ConfigurationException {
        List<Config> configs = new ArrayList<Config>();
        for (ConfigLoader loader : loaders) {
            if (loader.canLoad(url)) {
                Config config = loader.load(name, url);
                if (config != null) {
                    configs.add(config);
                    return;
                }
            }
        }
        
        throw new ConfigurationException("Configuration not found " + url);
    }
    
    public void load(String name, File file) throws ConfigurationException {
        List<Config> configs = new ArrayList<Config>();
        for (ConfigLoader loader : loaders) {
            if (loader.canLoad(file)) {
                Config config = loader.load(name, file);
                if (config != null) {
                    configs.add(config);
                    return;
                }
            }
        }
        
        throw new ConfigurationException("Configuration not found " + file.getAbsolutePath());
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
            public Config load(String resourceName) {
                List<Config> configs = new ArrayList<Config>();
                for (String resourcePermutationName : strategy.generate(resourceName, getStrInterpolator())) {
                    for (ConfigLoader loader : loaders) {
                        if (loader.canLoad(name)) {
                            Config config;
                            try {
                                configs.add(loader.load(name, resourcePermutationName));
                            } catch (ConfigurationException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
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
}
