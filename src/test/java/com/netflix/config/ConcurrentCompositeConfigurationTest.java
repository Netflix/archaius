package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Test;

public class ConcurrentCompositeConfigurationTest {
    @Test
    public void testProperties() {
        ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();        
        DynamicPropertyFactory factory = DynamicPropertyFactory.initWithConfigurationSource(config);
        DynamicStringProperty prop1 = factory.getStringProperty("prop1", null);
        DynamicStringProperty prop2 = factory.getStringProperty("prop2", null);
        DynamicStringProperty prop3 = factory.getStringProperty("prop3", null);
        DynamicStringProperty prop4 = factory.getStringProperty("prop4", null);
        AbstractConfiguration overrideConfig = new ConcurrentMapConfiguration();
        overrideConfig.addProperty("prop1", "prop1");
        overrideConfig.addProperty("prop2", "prop2");
        AbstractConfiguration baseConfig = new ConcurrentMapConfiguration();
        baseConfig.addProperty("prop3", "prop3");
        baseConfig.addProperty("prop1", "prop1FromBase");
        config.addConfiguration(overrideConfig, "runtime override", true);
        config.addConfiguration(baseConfig, "base configuration");
        assertEquals("prop1", config.getProperty("prop1"));
        assertEquals("prop1", prop1.get());
        assertEquals("prop2", prop2.get());
        assertEquals("prop3", prop3.get());
        overrideConfig.setProperty("prop1", "newvalue");
        assertEquals("newvalue", prop1.get());
        assertEquals("newvalue", config.getProperty("prop1"));
        baseConfig.addProperty("prop4", "prop4");
        assertEquals("prop4", config.getProperty("prop4"));
        assertEquals("prop4", prop4.get());
        baseConfig.setProperty("prop1", "newvaluefrombase");
        assertEquals("newvalue", prop1.get());
        overrideConfig.clearProperty("prop1");
        assertEquals("newvaluefrombase", config.getProperty("prop1"));
        assertEquals("newvaluefrombase", prop1.get());
        
        
    }

}
