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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

import com.netflix.archaius.cascade.SimpleCascadeStrategy;
import com.netflix.archaius.config.CascadingCompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

/**
 * DefaultConfigLoader provides a DSL to load configurations.
 * 
 * @author elandau
 *
 */
public class DefaultConfigLoader implements ConfigLoader {
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();

    public static class Builder {
        private List<ConfigReader>  loaders = new ArrayList<ConfigReader>();
        private CascadeStrategy     defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean             failOnFirst = true;
        private String              includeKey = "@next";
        private StrInterpolator     interpolator;
        
        public Builder withConfigReader(ConfigReader loader) {
            this.loaders.add(loader);
            return this;
        }
        
        public Builder withDefaultCascadingStrategy(CascadeStrategy strategy) {
            if (strategy != null) {
                this.defaultStrategy = strategy;
            }
            return this;
        }

        public Builder withFailOnFirst(boolean flag) {
            this.failOnFirst = flag;
            return this;
        }
        
        public Builder withStrInterpolator(StrInterpolator interpolator) {
            if (interpolator != null) 
                this.interpolator = interpolator;
            return this;
        }
        
        public Builder withConfigReader(List<ConfigReader> loaders) {
            if (loaders != null)
                this.loaders = loaders;
            return this;
        }
        
        public Builder withIncludeKey(String key) {
            if (key != null)
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
    
    private final List<ConfigReader> loaders;
    private final CascadeStrategy    defaultStrategy;
    private final String             includeKey;
    private final StrInterpolator    interpolator;
    private final CopyOnWriteArraySet<String> alreadyLoaded;
    private final boolean            defaultFailOnFirst;
    
    public DefaultConfigLoader(Builder builder) {
        this.includeKey         = builder.includeKey;
        this.loaders            = builder.loaders;
        this.defaultStrategy    = builder.defaultStrategy;
        this.interpolator       = builder.interpolator;
        this.alreadyLoaded      = new CopyOnWriteArraySet<String>();
        this.defaultFailOnFirst = builder.failOnFirst;
    }
    
    @Override
    public Loader newLoader() {
        return new Loader() {
            private CascadeStrategy strategy = defaultStrategy;
            private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            private String name;
            private boolean failOnFirst = defaultFailOnFirst;
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
            public Loader withOverrides(Properties props) {
                this.overrides = props;
                return this;
            }

            @Override
            public Loader withLoadToSystem(boolean toSystem) {
                loadToSystem = toSystem;
                return this;
            }

            @Override
            public Config load(String resourceName) {
                if (name == null) {
                    name = resourceName;
                }
                
                List<Config> configs = new ArrayList<Config>();
                boolean failIfNotLoaded = failOnFirst;
                for (String resourcePermutationName : strategy.generate(resourceName, interpolator)) {
                    for (ConfigReader loader : loaders) {
                        if (loader.canLoad(classLoader, name)) {
                            Config config;
                            String fileToLoad = resourcePermutationName;
                            do {
                                try {
                                    config = loader.load(classLoader, resourcePermutationName, resourcePermutationName);
                                    try {
                                        fileToLoad = config.getString(includeKey);
                                    }
                                    catch (Exception e) {
                                        // TODO: 
                                        fileToLoad = null;
                                    }
                                    configs.add(config);
                                } catch (ConfigException e) {
                                    break;
                                }
                            } while (fileToLoad != null);
                        }
                    }
                    
                    if (failIfNotLoaded == true) {
                        if (configs.isEmpty())
                            throw new RuntimeException("Failed to load configuration resource '" + resourceName + "'");
                        failIfNotLoaded = false;
                    }
                }
                
                if (overrides != null && !overrides.isEmpty()) {
                    configs.add(new MapConfig(name, overrides));
                }
                
                final Config config;
                // none
                if (configs.isEmpty()) {
                    return null;
                }
                // single
                else if (configs.size() == 1) {
                    config = configs.get(0);
                }
                // multiple
                else {
                    CascadingCompositeConfig cConfig = new CascadingCompositeConfig(name);
                    try {
                        cConfig.addConfigsLast(configs);
                    } catch (ConfigException e) {
                        throw new RuntimeException(e);
                    }
                    config = cConfig;
                }
                
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
                for (ConfigReader loader : loaders) {
                    if (loader.canLoad(classLoader, name)) {
                        try {
                            return loader.load(classLoader, name, url);
                        } catch (ConfigException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            public Config load(File file) throws ConfigException {
                try {
                    return load(file.toURL());
                } catch (MalformedURLException e) {
                    throw new ConfigException("Failed to load file " + file, e);
                }
            }
        };
    }    
}
