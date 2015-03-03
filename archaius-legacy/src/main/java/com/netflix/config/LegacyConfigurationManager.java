package com.netflix.config;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;

public interface LegacyConfigurationManager {

    void loadAppOverrideProperties(String appConfigName) throws IOException;

    void loadPropertiesFromResources(String path) throws IOException;

    void loadCascadedPropertiesFromResources(String configName) throws IOException;

    void loadPropertiesFromConfiguration(AbstractConfiguration config);

    void loadProperties(Properties properties);

    AbstractConfiguration getConfigInstance();

    DeploymentContext getDeploymentContext();

}
