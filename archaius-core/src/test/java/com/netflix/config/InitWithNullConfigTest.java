package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

public class InitWithNullConfigTest {
    
    @BeforeClass
    public static void init() {
        System.setProperty(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG, "true");
    }
    
    @Test
    public void testCreateProperty() {
        try {
            DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        DynamicBooleanProperty prop = DynamicPropertyFactory.getInstance().getBooleanProperty("abc", false);
        BaseConfiguration baseConfig = new BaseConfiguration();
        DynamicPropertyFactory.initWithConfigurationSource(baseConfig);
        baseConfig.setProperty("abc", "true");
        assertTrue(prop.get());
    }
}
