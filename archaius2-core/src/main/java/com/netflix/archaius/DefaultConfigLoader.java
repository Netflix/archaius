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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.StrInterpolator.Lookup;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.loaders.PropertiesConfigReader;

/**
 * DefaultConfigLoader provides a DSL to load configurations.
 * 
 * @author elandau
 *
 */
public class DefaultConfigLoader implements ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigLoader.class);

    private static final NoCascadeStrategy DEFAULT_CASCADE_STRATEGY = new NoCascadeStrategy();
    private static final Lookup DEFAULT_LOOKUP  = new Lookup() {
                                                        @Override
                                                        public String lookup(String key) {
                                                            return null;
                                                        }
                                                    };
    private static final StrInterpolator DEFAULT_INTERPOLATOR = CommonsStrInterpolator.INSTNACE;
                                                    
    public static class Builder {
        private List<ConfigReader>  loaders         = new ArrayList<ConfigReader>();
        private CascadeStrategy     defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean             failOnFirst     = true;
        private String              includeKey      = "@next";
        private StrInterpolator     interpolator    = DEFAULT_INTERPOLATOR;
        private Lookup              lookup          = DEFAULT_LOOKUP;
        
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
        
        public Builder withStrLookup(StrInterpolator.Lookup lookup) {
            this.lookup = lookup;
            return this;
        }
        
        public Builder withStrLookup(Config config) {
            this.lookup = ConfigStrLookup.from(config);
            return this;
        }
        
        public Builder withConfigReader(Set<ConfigReader> loaders) {
            if (loaders != null)
                this.loaders.addAll(loaders);
            return this;
        }
        
        public Builder withConfigReader(List<ConfigReader> loaders) {
            if (loaders != null)
                this.loaders.addAll(loaders);
            return this;
        }
        
        public Builder withIncludeKey(String key) {
            if (key != null)
                this.includeKey = key;
            return this;
        }
        
        public DefaultConfigLoader build() {
            if (loaders.isEmpty()) {
                loaders.add(new PropertiesConfigReader());
            }
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
    private final Lookup lookup;
    private final CopyOnWriteArraySet<String> alreadyLoaded;
    private final boolean            defaultFailOnFirst;
    
    public DefaultConfigLoader(Builder builder) {
        this.includeKey         = builder.includeKey;
        this.loaders            = builder.loaders;
        this.defaultStrategy    = builder.defaultStrategy;
        this.interpolator       = builder.interpolator;
        this.alreadyLoaded      = new CopyOnWriteArraySet<String>();
        this.defaultFailOnFirst = builder.failOnFirst;
        this.lookup             = builder.lookup;
    }
    
    @Override
    public Loader newLoader() {
        return new Loader() {
            private CascadeStrategy strategy = defaultStrategy;
            private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            private boolean failOnFirst = defaultFailOnFirst;
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
            public Config load(String resourceName) throws ConfigException {
                LinkedHashMap<String, Config> configs = new LinkedHashMap<String, Config>();
                boolean failIfNotLoaded = failOnFirst;
                for (String permutationName : strategy.generate(resourceName, interpolator, lookup)) {
                    LOGGER.info("Attempting to load {}", permutationName);
                    for (ConfigReader loader : loaders) {
                        if (loader.canLoad(classLoader, permutationName)) {
                            Config config;
                            String fileToLoad = permutationName;
                            do {
                                try {
                                    config = loader.load(classLoader, permutationName);
                                    try {
                                        fileToLoad = config.getString(includeKey);
                                    }
                                    catch (Exception e) {
                                        // TODO:
                                        fileToLoad = null;
                                    }
                                    configs.put(permutationName, config);
                                } catch (ConfigException e) {
                                    LOGGER.debug("could not load config '{}'. '{}'", new Object[]{fileToLoad, e.getMessage()});
                                    break;
                                }
                            } while (fileToLoad != null);
                        }
                    }
                    
                    if (failIfNotLoaded == true) {
                        if (configs.isEmpty())
                            throw new ConfigException("Failed to load configuration resource '" + resourceName + "'");
                        failIfNotLoaded = false;
                    }
                }
                
                if (overrides != null && !overrides.isEmpty()) {
                    configs.put(resourceName, new MapConfig(overrides));
                }
                
                // none
                if (configs.isEmpty()) {
                    return null;
                }
                // single
                else if (configs.size() == 1) {
                    return configs.values().iterator().next();
                }
                // multiple
                else {
                    CompositeConfig cConfig = new CompositeConfig();
                    ArrayList<String> names = new ArrayList<String>();
                    names.addAll(configs.keySet());
                    Collections.reverse(names);
                    
                    for (String name : names) {
                        cConfig.addConfig(name, configs.get(name));
                    }
                    return cConfig;
                }
            }

            @Override
            public Config load(URL url) {
                for (ConfigReader loader : loaders) {
                    if (loader.canLoad(classLoader, url)) {
                        try {
                            return loader.load(classLoader, url);
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
                    return load(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new ConfigException("Failed to load file " + file, e);
                }
            }
        };
    }    
}
