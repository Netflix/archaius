package com.netflix.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ConfigurationManager provides static access to the RootConfig.  
 * 
 * With wide adoption of dependency injection static management of RootConfig
 * is considered a bad practice and is only provided here for backward 
 * compatibility.
 * 
 * @author elandau
 */
@Deprecated
public class ConfigurationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    
    private static final String CONFIGURATION_FACTORY_PROP_NAME = "archaius.default.configuration.factory.class";
    private static final String DEFAULT_CONFIGURATION_FACTORY   = DefaultLegacyRootConfigurationFactory.class.getName();
    
    private volatile static LegacyConfigurationManager config;

    public static void setInstance(LegacyConfigurationManager local) {
        synchronized (ConfigurationManager.class) {
            if (config != null) {
                LOG.warn("ConfigurationManager already set");
            }
            config = local;
        }
    }
    
    public static LegacyConfigurationManager getInstance() {
        if (config == null) {
            synchronized (ConfigurationManager.class) {
                if (config == null) {
                    try {
                        String className = System.getProperty(CONFIGURATION_FACTORY_PROP_NAME, DEFAULT_CONFIGURATION_FACTORY);
                        if (className != null) {
                            config = ((LegacyRootConfigurationFactory) Class.forName(className).newInstance()).create();
                        } 
                    } 
                    catch (Exception e) {
                        throw new RuntimeException("Error initializing configuration", e);
                    }     
                }
            }
        }
        return config;
    }
    
    public static void clearInstance() {
        config = null;
    }
    
    public static AbstractConfiguration getConfigInstance() {
        return getInstance().getConfigInstance();
    }
    
    public static void loadPropertiesFromResources(String path) throws IOException {
        getInstance().loadPropertiesFromResources(path);
    }
    
    public static void loadCascadedPropertiesFromResources(String configName) throws IOException {
        getInstance().loadCascadedPropertiesFromResources(configName);
    }
    
    public static void loadAppOverrideProperties(String appConfigName) throws IOException {
        getInstance().loadAppOverrideProperties(appConfigName);
    }
    
    public static void loadPropertiesFromConfiguration(AbstractConfiguration abstractConfig) {
        getInstance().loadPropertiesFromConfiguration(abstractConfig);
    }
    
    public static void loadProperties(Properties properties) {
        getInstance().loadProperties(properties);
    }
    
    public static DeploymentContext getDeploymentContext() {
        return getInstance().getDeploymentContext();
    }
    
    public static AbstractConfiguration getConfigFromPropertiesFile(URL startingUrl) throws FileNotFoundException {
        return null;
    }

    public static Properties getPropertiesFromFile(URL startingUrl) throws FileNotFoundException {
        return null;
    }
    
    /**
     * Extract the configuration name as the simple filename portion of a URL
     * @param url
     * @return
     */
    private static String getConfigName(URL url) {
        String name = url.toExternalForm();
        name = name.replace('\\', '/'); // Windows
        final String scheme = url.getProtocol().toLowerCase();
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
        } 
        else {
            // Use the URL of the enclosing directory.
            final int slash = name.lastIndexOf("/");
            if (slash >= 0) {
                name = name.substring(0, slash);
            }
        }
        return name;
    }

}
