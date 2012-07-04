package com.netflix.config;

import java.io.IOException;
import java.io.InputStream;
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

public class ConfigurationManager {
    
    static volatile AbstractConfiguration instance = null;
    static volatile boolean configurationInstalled = false;
    private static volatile ConfigMBean configMBean = null;
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    
    static {
        String className = System.getProperty("archaius.configuration.class");
        if (className != null) {
            try {
                instance = (AbstractConfiguration) Class.forName(className).newInstance();
                configurationInstalled = true;
            } catch (Exception e) {
                throw new RuntimeException("Error initializing configuration", e);
            }
        } 
    }
    
    public static synchronized void install(AbstractConfiguration config) throws IllegalStateException {
        if (!configurationInstalled) {
            if (instance != null) {
                removeDefaultConfiguration();
            }
            if (DynamicPropertyFactory.getBackingConfigurationSource() != config) {                
                DynamicPropertyFactory.initWithConfigurationSource(config);
            }
            instance = config;
            configurationInstalled = true;
            if (Boolean.getBoolean(DynamicPropertyFactory.ENABLE_JMX)) {
                try {
                    configMBean = ConfigJMXManager.registerConfigMbean(instance);
                } catch (Exception e) {
                    logger.error("Unable to register with JMX", e);
                }
            }
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
                    if (Boolean.getBoolean(DynamicPropertyFactory.ENABLE_JMX)) {
                        try {
                            configMBean = ConfigJMXManager.registerConfigMbean(instance);
                        } catch (Exception e) {
                            logger.error("Unable to register with JMX", e);
                        }
                    }

                }
            }
        }
        return instance;
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
