/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.jmx.ConfigJMXManager;
import com.netflix.config.jmx.ConfigMBean;
import com.netflix.config.util.ConfigurationUtils;

/**
 * 
 * 
 * @author awang
 *
 */
public class ConfigurationManager {
    
    static volatile AbstractConfiguration instance = null;
    static volatile boolean configurationInstalled = false;
    private static volatile ConfigMBean configMBean = null;
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    static volatile DeploymentContext context = null;
    
    static {
        try {
            String className = System.getProperty("archaius.default.configuration.class");
            if (className != null) {
                instance = (AbstractConfiguration) Class.forName(className).newInstance();
                configurationInstalled = true;
            } else {
                String factoryName = System.getProperty("archaius.default.configuration.factory");
                if (factoryName != null) {
                    Method m = Class.forName(factoryName).getDeclaredMethod("getInstance", new Class[]{});
                    m.setAccessible(true);
                    instance = (AbstractConfiguration) m.invoke(null, new Object[]{});
                    configurationInstalled = true;
                }
            }
            String contextClassName = System.getProperty("archaius.default.deploymentContext.class");
            if (contextClassName != null) {
                context = (DeploymentContext) Class.forName(className).newInstance();
            } else {
                String factoryName = System.getProperty("archaius.default.deploymentContext.factory");
                if (factoryName != null) {
                    Method m = Class.forName(factoryName).getDeclaredMethod("getInstance", new Class[]{});
                    m.setAccessible(true);
                    context = (DeploymentContext) m.invoke(null, new Object[]{});
                } else {
                    context = new ConfigurationBasedDeploymentContext();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error initializing configuration", e);
        }
    }
    
    public static synchronized void install(AbstractConfiguration config) throws IllegalStateException {
        if (!configurationInstalled) {
            if (instance != null) {
                removeDefaultConfiguration();
            }
            instance = config;
            if (DynamicPropertyFactory.getBackingConfigurationSource() != config) {                
                DynamicPropertyFactory.initWithConfigurationSource(config);
            }
            configurationInstalled = true;
            registerConfigBean();
        } else {
            throw new IllegalStateException("A non-default configuration is already installed");
        }
    }

    public static synchronized boolean isConfigurationInstalled() {
        return configurationInstalled;
    }
    
    public static AbstractConfiguration getConfigInstance() {
        if (instance == null && !Boolean.getBoolean(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG)) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConcurrentCompositeConfiguration();            
                    if (!Boolean.getBoolean(DynamicPropertyFactory.DISABLE_DEFAULT_SYS_CONFIG)) {
                        SystemConfiguration sysConfig = new SystemConfiguration();                
                        ((ConcurrentCompositeConfiguration) instance).addConfiguration(sysConfig, DynamicPropertyFactory.SYS_CONFIG_NAME);
                        try {
                            DynamicURLConfiguration defaultURLConfig = new DynamicURLConfiguration();
                            ((ConcurrentCompositeConfiguration) instance).addConfiguration(defaultURLConfig, DynamicPropertyFactory.URL_CONFIG_NAME);
                        } catch (Throwable e) {
                            logger.warn("Failed to create default dynamic configuration", e);
                        }
                    }
                    registerConfigBean();
                }
            }
        }
        return instance;
    }
    
    static void registerConfigBean() {
        if (Boolean.getBoolean(DynamicPropertyFactory.ENABLE_JMX)) {
            try {
                configMBean = ConfigJMXManager.registerConfigMbean(instance);
            } catch (Exception e) {
                logger.error("Unable to register with JMX", e);
            }
        }        
    }
    
    static void setDirect(AbstractConfiguration config) {
        ConfigurationManager.removeDefaultConfiguration();
        ConfigurationManager.instance = config;
        ConfigurationManager.configurationInstalled = true;
        ConfigurationManager.registerConfigBean();
    }
    
    public static void loadPropertiesFromResources(String path) 
            throws IOException {
        if (instance == null) {
            instance = getConfigInstance();
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(path);
        Properties props = new Properties();
        InputStream fin = url.openStream();
        props.load(fin);
        fin.close();
        if (instance instanceof AggregatedConfiguration) {
            String name = getConfigName(url);
            ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
            config.loadProperties(props);
            ((AggregatedConfiguration) instance).addConfiguration(config, name);
        } else {
            ConfigurationUtils.loadProperties(props, instance);
        }
    }

    public static void loadPropertiesFromConfiguration(AbstractConfiguration config) {
        if (instance instanceof AggregatedConfiguration) {
            ((AggregatedConfiguration) instance).addConfiguration(config);
        } else {
            Properties props = ConfigurationUtils.getProperties(config);
            ConfigurationUtils.loadProperties(props, instance);
        }        
    }
    
    public static void loadProperties(Properties properties) {
        ConfigurationUtils.loadProperties(properties, instance);
    }
    
    public static void setDeploymentContext(DeploymentContext context) {
        ConfigurationManager.context = context;
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
    
    static synchronized void removeDefaultConfiguration() {
        if (instance == null || configurationInstalled) {
            return;
        }
        ConcurrentCompositeConfiguration defaultConfig = (ConcurrentCompositeConfiguration) instance;
        // stop loading of the configuration
        DynamicURLConfiguration defaultFileConfig = (DynamicURLConfiguration) defaultConfig.getConfiguration(DynamicPropertyFactory.URL_CONFIG_NAME);
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
}
