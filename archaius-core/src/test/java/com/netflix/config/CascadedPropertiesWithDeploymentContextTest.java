package com.netflix.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CascadedPropertiesWithDeploymentContextTest {

    //@Test
    public void testLoadCascadedPropertiesConfigDeployment() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty("archaius.deployment.environment", "test");
        ConfigurationManager.getConfigInstance().setProperty("archaius.deployment.region", "us-east-1");
        ConfigurationManager.loadCascadedPropertiesFromResources("test");
        assertEquals("9", ConfigurationManager.getConfigInstance().getProperty("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
        assertEquals("1", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
        ConfigurationManager.loadAppOverrideProperties("override");
        assertEquals("200", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
    }
}
