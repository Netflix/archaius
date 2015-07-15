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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.StrInterpolator.Lookup;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import com.netflix.archaius.readers.PropertiesConfigReader;

/**
 * DefaultConfigLoader provides a DSL to load configurations.
 * 
 * @author elandau
 *
 */
public class DefaultConfigLoader implements ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConfigLoader.class);

    private static final NoCascadeStrategy DEFAULT_CASCADE_STRATEGY = new NoCascadeStrategy();
    private static final Lookup DEFAULT_LOOKUP  = new Lookup() {
                                                        @Override
                                                        public String lookup(String key) {
                                                            return null;
                                                        }
                                                    };
    private static final StrInterpolator DEFAULT_INTERPOLATOR = CommonsStrInterpolator.INSTANCE;
                                                    
    public static class Builder {
        private List<ConfigReader>  loaders         = new ArrayList<ConfigReader>();
        private CascadeStrategy     defaultStrategy = DEFAULT_CASCADE_STRATEGY;
        private boolean             failOnFirst     = false;
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
    private final StrInterpolator    interpolator;
    private final Lookup             lookup;
    private final boolean            defaultFailOnFirst;
    
    public DefaultConfigLoader(Builder builder) {
        this.loaders            = builder.loaders;
        this.defaultStrategy    = builder.defaultStrategy;
        this.interpolator       = builder.interpolator;
        this.defaultFailOnFirst = builder.failOnFirst;
        this.lookup             = builder.lookup;
    }
    
    @Override
    public Loader newLoader() {
        return new Loader() {
            private CascadeStrategy strategy = defaultStrategy;
            private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            private boolean failOnFirst = defaultFailOnFirst;
            private Config overrides = null;
            
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
                this.overrides = MapConfig.from(props);
                return this;
            }

            @Override
            public Loader withOverrides(Config config) {
                this.overrides = config;
                return this;
            }

            @Override
            public LinkedHashMap<String, Config> load(String resourceName) throws ConfigException {
                LinkedHashMap<String, Config> configs = new LinkedHashMap<String, Config>();
                boolean failIfNotLoaded = failOnFirst;
                if (overrides != null) {
                    LOG.debug("Loading overrides form {}", resourceName);
                    configs.put(resourceName + "_overrides", overrides);
                }
                
                List<String> names = strategy.generate(resourceName, interpolator, lookup);
                for (String name : names) {
                    for (ConfigReader reader : loaders) {
                        if (reader.canLoad(classLoader, name)) {
                            try {
                                configs.put(name, reader.load(classLoader, name));
                                LOG.debug("Loaded {} ", name);
                                failIfNotLoaded = false;
                            }
                            catch (ConfigException e) {
                                LOG.debug("Unable to load {}, {}", name, e.getMessage());
                                if (failIfNotLoaded == true) {
                                    throw new ConfigException("Failed to load configuration resource '" + resourceName + "'");
                                }
                            }
                            break;
                        }
                    }
                }
                
                return configs;
            }
 
            @Override
            public Config load(URL url) {
                for (ConfigReader loader : loaders) {
                    if (loader.canLoad(classLoader, url)) {
                        try {
                            Config config = loader.load(classLoader, url);
                            LOG.info("Loaded " + url);
                            return config;
                        } catch (ConfigException e) {
                            LOG.info("Unable to load file '{}'", new Object[]{url, e.getMessage()});
                        } catch (Exception e) {
                            LOG.info("Unable to load file '{}'", new Object[]{url, e.getMessage()});
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
