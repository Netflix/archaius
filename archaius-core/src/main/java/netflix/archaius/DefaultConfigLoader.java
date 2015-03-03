package netflix.archaius;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

import netflix.archaius.cascade.SimpleCascadeStrategy;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigurationException;

/**
 * DefaultConfigLoader provides a DSL to load configurations.
 * 
 * @author elandau
 *
 */
public class DefaultConfigLoader {
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();

    /**
     * DSL for loading a configuration
     * 
     * @author elandau
     *
     */
    public static interface Loader {
        /**
         * Cascading policy to use the loading based on a resource name.  All loaded
         * files will be merged into a single Config.
         * @param strategy
         */
        Loader withCascadeStrategy(CascadeStrategy strategy);
        
        /**
         * Arbitrary name assigned to the loaded Config.
         * @param name
         */
        Loader withName(String name);
        
        /**
         * Class loader to use
         * @param loader
         */
        Loader withClassLoader(ClassLoader loader);
        
        /**
         * When true, fail the entire load operation if the first resource name
         * can't be loaded.  By definition all cascaded variations are treated 
         * as overrides
         * @param flag
         */
        Loader withFailOnFirst(boolean flag);
        
        /**
         * Externally provided property overrides that are applied once 
         * all cascaded files have been loaded
         * 
         * @param props
         */
        Loader withOverrides(Properties props);
        
        /**
         * Once loaded add all the properties to System.setProperty()
         * @param toSystem
         * @return
         */
        Loader withLoadToSystem(boolean toSystem);
        
        Config load(String resourceName);
        Config load(URL url);
    }

    public static class Builder {
        private List<ConfigLoader>  loaders = new ArrayList<ConfigLoader>();
        private CascadeStrategy     defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean             failOnFirst = true;
        private String              name = "";
        private String              includeKey = "@next";
        private StrInterpolator     interpolator;
        
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
        
        public Builder withStrInterpolator(StrInterpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }
        
        public Builder withConfigLoaders(List<ConfigLoader> loaders) {
            this.loaders = loaders;
            return this;
        }
        
        public Builder withIncludeKey(String key) {
            this.includeKey = key;
            return this;
        }
        
        public DefaultConfigLoader build() {
            return new DefaultConfigLoader(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final List<ConfigLoader> loaders;
    private final CascadeStrategy    defaultStrategy;
    private final String             includeKey;
    private final StrInterpolator    interpolator;
    private final CopyOnWriteArraySet<String> alreadyLoaded;
    
    public DefaultConfigLoader(Builder builder) {
        this.includeKey      = builder.includeKey;
        this.loaders         = builder.loaders;
        this.defaultStrategy = builder.defaultStrategy;
        this.interpolator    = builder.interpolator;
        this.alreadyLoaded   = new CopyOnWriteArraySet<String>();
    }

    public void load(String name, URL url) throws ConfigurationException {
        if (alreadyLoaded.contains(url)) 
            return;
        
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
    
    public Config load(String name, File file) throws ConfigurationException {
        for (ConfigLoader loader : loaders) {
            if (loader.canLoad(file)) {
                Config config = loader.load(name, file);
                if (config != null) {
                    return config;
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
            private boolean loadToSystem = false;
            private Properties overrides = null;
            
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
                for (String resourcePermutationName : strategy.generate(resourceName, interpolator)) {
                    for (ConfigLoader loader : loaders) {
                        if (loader.canLoad(name)) {
                            Config config;
                            String fileToLoad = resourcePermutationName;
                            do {
                                try {
                                    config = loader.load(name, resourcePermutationName);
                                    fileToLoad = config.getString(includeKey);
                                    configs.add(config);
                                } catch (ConfigurationException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            } while (fileToLoad != null);
                        }
                    }
                }
                
                if (overrides != null && !overrides.isEmpty()) {
                    configs.add(new MapConfig(name, overrides));
                }
                
                Config config = configs.isEmpty() 
                        ? null                             // none 
                        : configs.size() == 1
                           ? configs.get(0)                // one 
                           : new MapConfig(name, configs); // multiple
                
                if (loadToSystem) {
                    Iterator<String> keys = config.getKeys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        System.setProperty(key, config.getString(key));
                    }
                }
                return config;
            }

            @Override
            public Config load(URL url) {
                for (ConfigLoader loader : loaders) {
                    if (loader.canLoad(name)) {
                        try {
                            return loader.load(name, url);
                        } catch (ConfigurationException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            public Loader withOverrides(Properties props) {
                this.overrides = props;
                return this;
            }

            @Override
            public Loader withLoadToSystem(boolean toSystem) {
                loadToSystem = toSystem;
                return this;
            }
        };
    }    
}
