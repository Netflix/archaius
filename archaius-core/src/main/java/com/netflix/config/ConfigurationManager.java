/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config;

import com.netflix.config.jmx.ConfigJMXManager;
import com.netflix.config.jmx.ConfigMBean;
import com.netflix.config.util.ConfigurationUtils;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The configuration manager is a central place where it manages the system wide Configuration and
 * deployment context.
 * <p>
 * During initialization, this class will check system property "archaius.default.configuration.class"
 * and "archaius.default.configuration.factory". If the former is set, it will use the class name to instantiate 
 * it using its default no-arg constructor. If the later is set, it will call its static method getInstance().
 * In both cases, the returned Configuration object will be set as the system wide configuration.
 * 
 * @author awang
 *
 */
public class ConfigurationManager {
    
    static volatile AbstractConfiguration instance = null;
    static volatile boolean customConfigurationInstalled = false;
    private static volatile ConfigMBean configMBean = null;
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    static volatile DeploymentContext context = null;
    
    /**
     * initStack will hold the stack trace at the time of static initialization for ConfigurationManager
     * to help debug where/when ConfigurationManager was created.  We do this to help debug issues with
     * Archaius2 where ConfigurationManager is initialized before the bridge has been properly set up
     */
    private static StackTraceElement[] initStack = null;
    
    public static final String PROP_NEXT_LOAD = "@next";
    public static final String URL_CONFIG_NAME = "archaius.dynamicPropertyFactory.URL_CONFIG";
    public static final String SYS_CONFIG_NAME = "archaius.dynamicPropertyFactory.SYS_CONFIG";
    public static final String ENV_CONFIG_NAME = "archaius.dynamicPropertyFactory.ENV_CONFIG";

    /**
     * System property to disable adding EnvironmentConfiguration to the default ConcurrentCompositeConfiguration
     */
    public static final String DISABLE_DEFAULT_ENV_CONFIG = "archaius.dynamicProperty.disableEnvironmentConfig";

    /**
     * System property to disable adding SystemConfiguration to the default ConcurrentCompositeConfiguration
     */
    public static final String DISABLE_DEFAULT_SYS_CONFIG = "archaius.dynamicProperty.disableSystemConfig";


    private static final String PROP_NEXT_LOAD_NFLX = "netflixconfiguration.properties.nextLoad";
    public static final String APPLICATION_PROPERTIES = "APPLICATION_PROPERTIES";
    
    private static Set<String> loadedPropertiesURLs = new CopyOnWriteArraySet<String>();
    
    static {
        initStack = Thread.currentThread().getStackTrace();
        try {
            String className = System.getProperty("archaius.default.configuration.class");
            if (className != null) {
                instance = (AbstractConfiguration) Class.forName(className).newInstance();
                customConfigurationInstalled = true;
            } else {
                String factoryName = System.getProperty("archaius.default.configuration.factory");
                if (factoryName != null) {
                    Method m = Class.forName(factoryName).getDeclaredMethod("getInstance", new Class[]{});
                    m.setAccessible(true);
                    instance = (AbstractConfiguration) m.invoke(null, new Object[]{});
                    customConfigurationInstalled = true;
                }
            }
            String contextClassName = System.getProperty("archaius.default.deploymentContext.class");
            if (contextClassName != null) {
                setDeploymentContext((DeploymentContext) Class.forName(contextClassName).newInstance());
            } else {
                String factoryName = System.getProperty("archaius.default.deploymentContext.factory");
                if (factoryName != null) {
                    Method m = Class.forName(factoryName).getDeclaredMethod("getInstance", new Class[]{});
                    m.setAccessible(true);
                    setDeploymentContext((DeploymentContext) m.invoke(null, new Object[]{}));
                } else {
                    setDeploymentContext(new ConfigurationBasedDeploymentContext());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error initializing configuration", e);
        }
    }
    
    /**
     * @return Return the stack trace that triggered static initialization of ConfigurationManager.  This
     * information can be used to help debug static initialization issues with the Archaius2 bridge.
     */
    public static StackTraceElement[] getStaticInitializationSource() {
        return initStack;
    }
    
    public static Set<String> getLoadedPropertiesURLs() {
        return loadedPropertiesURLs;
    }

    /**
     * Install the system wide configuration with the ConfigurationManager. This will also install 
     * the configuration with the {@link DynamicPropertyFactory} by calling {@link DynamicPropertyFactory#initWithConfigurationSource(AbstractConfiguration)}.
     * This call can be made only once, otherwise IllegalStateException will be thrown.
     */
    public static synchronized void install(AbstractConfiguration config) throws IllegalStateException {
        if (!customConfigurationInstalled) {
            setDirect(config);
            if (DynamicPropertyFactory.getBackingConfigurationSource() != config) {                
                DynamicPropertyFactory.initWithConfigurationSource(config);
            }
        } else {
            throw new IllegalStateException("A non-default configuration is already installed");
        }
    }

    public static synchronized boolean isConfigurationInstalled() {
        return customConfigurationInstalled;
    }
    
    private static AbstractConfiguration createDefaultConfigInstance() {
        ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();  
        try {
            DynamicURLConfiguration defaultURLConfig = new DynamicURLConfiguration();
            config.addConfiguration(defaultURLConfig, URL_CONFIG_NAME);
        } catch (Throwable e) {
            logger.warn("Failed to create default dynamic configuration", e);
        }
        if (!Boolean.getBoolean(DISABLE_DEFAULT_SYS_CONFIG)) {
            SystemConfiguration sysConfig = new SystemConfiguration();
            config.addConfiguration(sysConfig, SYS_CONFIG_NAME);
        }
        if (!Boolean.getBoolean(DISABLE_DEFAULT_ENV_CONFIG)) {
            EnvironmentConfiguration envConfig = new EnvironmentConfiguration();
            config.addConfiguration(envConfig, ENV_CONFIG_NAME);
        }
        ConcurrentCompositeConfiguration appOverrideConfig = new ConcurrentCompositeConfiguration();
        config.addConfiguration(appOverrideConfig, APPLICATION_PROPERTIES);
        config.setContainerConfigurationIndex(config.getIndexOfConfiguration(appOverrideConfig));
        return config;
    }
    
    private static AbstractConfiguration getConfigInstance(boolean defaultConfigDisabled) {
        if (instance == null && !defaultConfigDisabled) {
            instance = createDefaultConfigInstance();
            registerConfigBean();
        }
        return instance;        
    }
    
    /**
     * Get the current system wide configuration. If there has not been set, it will return a default
     * {@link ConcurrentCompositeConfiguration} which contains a SystemConfiguration from Apache Commons
     * Configuration and a {@link DynamicURLConfiguration}.
     */
    public static AbstractConfiguration getConfigInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = getConfigInstance(Boolean.getBoolean(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG));
                }
            }
        }
        return instance;
    }
    
    private static void registerConfigBean() {
        if (Boolean.getBoolean(DynamicPropertyFactory.ENABLE_JMX)) {
            try {
                configMBean = ConfigJMXManager.registerConfigMbean(instance);
            } catch (Exception e) {
                logger.error("Unable to register with JMX", e);
            }
        }        
    }
    
    static synchronized void setDirect(AbstractConfiguration config) {
        if (instance != null) {
            Collection<ConfigurationListener> listeners = instance.getConfigurationListeners();
            // transfer listeners
            // transfer properties which are not in conflict with new configuration
            for (Iterator<String> i = instance.getKeys(); i.hasNext();) {
                String key = i.next();
                Object value = instance.getProperty(key);
                if (value != null && !config.containsKey(key)) {
                    config.setProperty(key, value);
                }
            }
            if (listeners != null) {
                for (ConfigurationListener listener: listeners) {
                    if (listener instanceof ExpandedConfigurationListenerAdapter
                            && ((ExpandedConfigurationListenerAdapter) listener).getListener() 
                            instanceof DynamicProperty.DynamicPropertyListener) {
                        // no need to transfer the fast property listener as it should be set later
                        // with the new configuration
                        continue;
                    }
                    config.addConfigurationListener(listener);
                }
            }
        }
        ConfigurationManager.removeDefaultConfiguration();
        ConfigurationManager.instance = config;
        ConfigurationManager.customConfigurationInstalled = true;
        ConfigurationManager.registerConfigBean();
    }
      
    /**
     * Load properties from resource file(s) into the system wide configuration
     * @param path relative path of the resources
     * @throws IOException
     */
    public static void loadPropertiesFromResources(String path) 
            throws IOException {
        if (instance == null) {
            instance = getConfigInstance();
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = loader.getResources(path);
        if (!resources.hasMoreElements()) {
            //non-existent config path. Throw an exception. Issue #150
            throw new IOException("Cannot locate " + path + " as a classpath resource.");
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            InputStream fin = url.openStream();
            Properties props = ConfigurationUtils.loadPropertiesFromInputStream(fin);
            if (instance instanceof AggregatedConfiguration) {
                String name = getConfigName(url);
                ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
                config.loadProperties(props);
                ((AggregatedConfiguration) instance).addConfiguration(config, name);
            } else {
                ConfigurationUtils.loadProperties(props, instance);
            }
        }
    }
    
    /**
     * Load resource configName.properties first. Then load configName-deploymentEnvironment.properties
     * into the system wide configuration. For example, if configName is "application", and deployment environment
     * is "test", this API will first load "application.properties", then load "application-test.properties" to
     * override any property that also exist in "application.properties". 
     * 
     * @param configName prefix of the properties file name.
     * @throws IOException
     * @see DeploymentContext#getDeploymentEnvironment()
     */
    public static void loadCascadedPropertiesFromResources(String configName) throws IOException {
        Properties props = loadCascadedProperties(configName);
        if (instance instanceof AggregatedConfiguration) {
            ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
            config.loadProperties(props);
            ((AggregatedConfiguration) instance).addConfiguration(config, configName);
        } else {
            ConfigurationUtils.loadProperties(props, instance);
        }
    }

    private static Properties loadCascadedProperties(String configName) throws IOException {
        String defaultConfigFileName = configName + ".properties";
        if (instance == null) {
            instance = getConfigInstance();
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(defaultConfigFileName);
        if (url == null) {
            throw new IOException("Cannot locate " + defaultConfigFileName + " as a classpath resource.");
        }
        Properties props = getPropertiesFromFile(url);
        String environment = getDeploymentContext().getDeploymentEnvironment();
        if (environment != null && environment.length() > 0) {
            String envConfigFileName = configName + "-" + environment + ".properties";
            url = loader.getResource(envConfigFileName);
            if (url != null) {
                Properties envProps = getPropertiesFromFile(url);
                if (envProps != null) {
                    props.putAll(envProps);
                }
            }
        }
        return props;
    }
    
    public static void loadAppOverrideProperties(String appConfigName) throws IOException {
        AbstractConfiguration config = getConfigInstance();
        Properties props = loadCascadedProperties(appConfigName);
        if (config instanceof AggregatedConfiguration) {
            AggregatedConfiguration aggregated = (AggregatedConfiguration) config;
            Configuration appConfig = aggregated.getConfiguration(APPLICATION_PROPERTIES);
            if (appConfig != null) {
                ConfigurationUtils.loadProperties(props, appConfig);
                return;
            } 
        }
        // The configuration instance is not an aggregated configuration or it does
        // not have designated configuration for application properties - just add
        // the properties using config.setProperty()
        ConfigurationUtils.loadProperties(props, config);        
    }
    
    /**
     * Load properties from the specified configuration into system wide configuration
     */
    public static void loadPropertiesFromConfiguration(AbstractConfiguration config) {
        if (instance == null) {
            instance = getConfigInstance();
        }
        if (instance instanceof AggregatedConfiguration) {
            ((AggregatedConfiguration) instance).addConfiguration(config);
        } else {
            Properties props = ConfigurationUtils.getProperties(config);
            ConfigurationUtils.loadProperties(props, instance);
        }        
    }
    
    /**
     * Load the specified properties into system wide configuration
     */
    public static void loadProperties(Properties properties) {
        if (instance == null) {
            instance = getConfigInstance();
        }
        ConfigurationUtils.loadProperties(properties, instance);
    }
    
    public static void setDeploymentContext(DeploymentContext context) {
        ConfigurationManager.context = context;
        if (getConfigInstance() == null) {
            return;
        }
        for (DeploymentContext.ContextKey key: DeploymentContext.ContextKey.values()) {
            String value = context.getValue(key);
            if (value != null) {
                instance.setProperty(key.getKey(), value);
            }
        }      
    }
    
    public static DeploymentContext getDeploymentContext() {
        return context;
    }
    
    private static String getConfigName(URL propertyFile)
    {
        String name = propertyFile.toExternalForm();
        name = name.replace('\\', '/'); // Windows
        final String scheme = propertyFile.getProtocol().toLowerCase();
        if ("jar".equals(scheme) || "zip".equals(scheme)) {
            // Use the unqualified name of the jar file.
            final int bang = name.lastIndexOf("!");
            if (bang >= 0) {
                name = name.substring(0, bang);
            }
            final int slash = name.lastIndexOf("/");
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
        } else {
            // Use the URL of the enclosing directory.
            final int slash = name.lastIndexOf("/");
            if (slash >= 0) {
                name = name.substring(0, slash);
            }
        }
        return name;
    }
    
    private static synchronized void removeDefaultConfiguration() {
        if (instance == null || customConfigurationInstalled) {
            return;
        }
        ConcurrentCompositeConfiguration defaultConfig = (ConcurrentCompositeConfiguration) instance;
        // stop loading of the configuration
        DynamicURLConfiguration defaultFileConfig = (DynamicURLConfiguration) defaultConfig.getConfiguration(URL_CONFIG_NAME);
        if (defaultFileConfig != null) {
            defaultFileConfig.stopLoading();
        }
        Collection<ConfigurationListener> listeners = defaultConfig.getConfigurationListeners();
        
        // find the listener and remove it so that DynamicProperty will no longer receives 
        // callback from the default configuration source
        ConfigurationListener dynamicPropertyListener = null;
        for (ConfigurationListener l: listeners) {
            if (l instanceof ExpandedConfigurationListenerAdapter
                    && ((ExpandedConfigurationListenerAdapter) l).getListener() 
                    instanceof DynamicProperty.DynamicPropertyListener) {
                dynamicPropertyListener = l;
                break;                        
            }
        }
        if (dynamicPropertyListener != null) {
            defaultConfig.removeConfigurationListener(dynamicPropertyListener);
        }
        if (configMBean != null) {
            try {
                ConfigJMXManager.unRegisterConfigMBean(defaultConfig, configMBean);
            } catch (Exception e) {
                logger.error("Error unregistering with JMX", e);
            }
        }
        instance = null;        
    }      
    
    public static AbstractConfiguration getConfigFromPropertiesFile(URL startingUrl) 
    throws FileNotFoundException {
        return ConfigurationUtils.getConfigFromPropertiesFile(startingUrl,  
                getLoadedPropertiesURLs(), PROP_NEXT_LOAD, PROP_NEXT_LOAD_NFLX);
    }
    
    public static Properties getPropertiesFromFile(URL startingUrl) 
    throws FileNotFoundException {
        return ConfigurationUtils.getPropertiesFromFile(startingUrl, getLoadedPropertiesURLs(), PROP_NEXT_LOAD, PROP_NEXT_LOAD_NFLX);
    }


}

