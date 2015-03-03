package com.netflix.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import netflix.archaius.CascadeStrategy;
import netflix.archaius.Config;
import netflix.archaius.ConfigLoader;
import netflix.archaius.DefaultConfigLoader;
import netflix.archaius.RootConfig;
import netflix.archaius.cascade.SimpleCascadeStrategy;
import netflix.archaius.commons.CommonsToConfig;
import netflix.archaius.config.CompositeConfig;
import netflix.archaius.config.EnvironmentConfig;
import netflix.archaius.config.MapConfig;
import netflix.archaius.config.SimpleDynamicConfig;
import netflix.archaius.config.SystemConfig;

import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.config.DeploymentContext.ContextKey;

/**
 * Apache Commons Configuration based adapter for the Archaius2 RootConfig to be used
 * in legacy code.
 * 
 * This implementation builds the following override structure
 * 
 *  Runtime Properties (including manually set properties)
 *  URL                (or polled configuration)
 *  System 
 *  Environment
 *  Application        (one set per application)
 *  Library            (called for each library being loaded)
 * 
 * @author elandau
 *
 */
public class DefaultLegacyConfigurationManager implements LegacyConfigurationManager {
    private static final SimpleCascadeStrategy DEFAULT_CASCADE_STRATEGY = new SimpleCascadeStrategy();
    
    public static final String RUNTIME_PROPERTIES                   = "RUNTIME_PROPERTIES";
    public static final String URL_CONFIG_NAME                      = "archaius.dynamicPropertyFactory.URL_CONFIG";
    public static final String SYS_CONFIG_NAME                      = "archaius.dynamicPropertyFactory.SYS_CONFIG";
    public static final String ENV_CONFIG_NAME                      = "archaius.dynamicPropertyFactory.ENV_CONFIG";
    public static final String APPLICATION_PROPERTIES               = "APPLICATION_PROPERTIES";
    public static final String LIBRARY_PROPERTIES                   = "LIBRARY_PROPERTIES";
    
    public static class Builder {
        private final List<ConfigLoader>  loaders            = new ArrayList<ConfigLoader>();
        private CascadeStrategy           defaultStrategy    = DEFAULT_CASCADE_STRATEGY;
        private boolean                   failOnFirst        = true;
        private boolean                   includeSystem      = true;
        private boolean                   includeEnvironment = true;
        private DeploymentContext         deploymentContext  = null;
            
        public Builder withSystemProperties(boolean flag) {
            this.includeSystem = flag;
            return this;
        }
        
        public Builder withEnvironmentProperties(boolean flag) {
            this.includeEnvironment = flag;
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
        
        public Builder withDeploymentContext(DeploymentContext context) {
            this.deploymentContext = context;
            return this;
        }
        
        public DefaultLegacyConfigurationManager build() {
            return new DefaultLegacyConfigurationManager(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private AbstractConfiguration   commonsConfig;
    private RootConfig              config;
    private CompositeConfig         application;
    private CompositeConfig         library;
    private SimpleDynamicConfig     runtime;
    private DefaultConfigLoader   loader;
    private DeploymentContext       deploymentContext;

    DefaultLegacyConfigurationManager(Builder builder) {
        config = RootConfig.builder().build();
        
        config.addConfigLast(runtime = new SimpleDynamicConfig(RUNTIME_PROPERTIES));
        
        if (builder.includeSystem) {
            config.addConfigLast(new SystemConfig(SYS_CONFIG_NAME));
        }
        
        if (builder.includeEnvironment) {
            config.addConfigLast(new EnvironmentConfig(ENV_CONFIG_NAME));
        }
        
        if (builder.deploymentContext != null) {
            this.deploymentContext = builder.deploymentContext;
            for (ContextKey key : DeploymentContext.ContextKey.values()) {
                String value = this.deploymentContext.getValue(key);
                if (value != null) {
                    runtime.setProperty(key.getKey(), value);
                }
            }
        }
        else {
            this.deploymentContext = new ConfigurationBasedDeploymentContext(config);
        }
        
        loader = DefaultConfigLoader.builder()
            .withConfigLoaders(builder.loaders)
            .withDefaultCascadingStrategy(builder.defaultStrategy)
            .withFailOnFirst(builder.failOnFirst)
            .withStrInterpolator(config.getStrInterpolator())
            .build();
        
        config.addConfigLast(application = new CompositeConfig(APPLICATION_PROPERTIES));
        config.addConfigLast(library = new CompositeConfig(LIBRARY_PROPERTIES));
        
        commonsConfig = new AbstractConfiguration() {
            @Override
            public boolean isEmpty() {
                return config.isEmpty();
            }

            @Override
            public boolean containsKey(String key) {
                return config.containsProperty(key);
            }

            @Override
            public Object getProperty(String key) {
                return config.getRawProperty(key);
            }

            @Override
            public Iterator<String> getKeys() {
                return config.getKeys();
            }

            @Override
            protected void addPropertyDirect(String key, Object value) {
                runtime.setProperty(key, value.toString());
            }
        };
    }
    
    @Override
    public AbstractConfiguration getConfigInstance() {
        return commonsConfig;
    }
    
    @Override
    public void loadAppOverrideProperties(String appConfigName) throws IOException {
        application.addConfigLast(loader.newLoader().load(appConfigName));
    }
    
    @Override
    public void loadPropertiesFromResources(String path) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);
        if (!resources.hasMoreElements()) {
            throw new IOException("Cannot locate " + path + " as a classpath resource.");
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            Config config = loader.newLoader().withClassLoader(classLoader).withName(path).load(url);
            if (config != null) {
                library.addConfigFirst(config);
            }
        }
    }
    
    @Override
    public void loadCascadedPropertiesFromResources(String configName) throws IOException {
        library.addConfigFirst(loader.newLoader().load(configName));
    }
    
    @Override
    public void loadPropertiesFromConfiguration(AbstractConfiguration config) {
        library.addConfigFirst(new CommonsToConfig(config));
    }
    
    @Override
    public void loadProperties(Properties properties) {
        library.addConfigFirst(new MapConfig("", properties));
    }

    @Override
    public DeploymentContext getDeploymentContext() {
        return deploymentContext;
    }
}
