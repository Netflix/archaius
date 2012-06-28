package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

public class ConfigurationManagerTest {

    static DynamicStringProperty prop1 = DynamicPropertyFactory.getInstance().getStringProperty("prop1", null);
    
    @Test
    public void testInstall() {
        ConfigurationManager.getConfigInstance().setProperty("prop1", "abc");
        assertEquals("abc", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("abc", prop1.get());
        BaseConfiguration newConfig = new BaseConfiguration();
        newConfig.setProperty("prop1", "fromNewConfig");
        ConfigurationManager.install(newConfig);
        assertEquals("fromNewConfig", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("fromNewConfig", prop1.get());
        newConfig.setProperty("prop1", "changed");
        assertEquals("changed", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("changed", prop1.get());
        try {
            ConfigurationManager.install(new BaseConfiguration());
            fail("IllegalStateExceptionExpected");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        try {
            DynamicPropertyFactory.initWithConfigurationSource(new BaseConfiguration());
            fail("IllegalStateExceptionExpected");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }
    
    @Test
    public void testLoadProperties() throws Exception {
        ConfigurationManager.loadPropertiesFromResources("sampleapp.properties");
        assertEquals("5", ConfigurationManager.getConfigInstance().getProperty("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
    }
}
