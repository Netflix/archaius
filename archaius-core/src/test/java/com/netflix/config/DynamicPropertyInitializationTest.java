package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.Test;

public class DynamicPropertyInitializationTest {
    private volatile Object lastModified;
    
    ConfigurationListener listener = new ConfigurationListener() {
        
        @Override
        public void configurationChanged(ConfigurationEvent arg0) {
            if (!arg0.isBeforeUpdate()) {
                lastModified = arg0.getPropertyValue();
            }            
        }    
                
    };
    
    @Test
    public void testDefaultConfig() {
        System.setProperty("xyz", "fromSystem");
        DynamicStringProperty prop = new DynamicStringProperty("xyz", null);        
        assertNotNull(DynamicPropertyFactory.getBackingConfigurationSource());
        assertEquals("fromSystem", prop.get());
        ConfigurationManager.getConfigInstance().addConfigurationListener(listener);
        ConfigurationManager.getConfigInstance().setProperty("xyz", "override");
        assertEquals("override", prop.get());
        assertEquals("override", lastModified);
        BaseConfiguration newConfig = new BaseConfiguration();
        newConfig.setProperty("xyz", "fromNewConfig");
        ConfigurationManager.install(newConfig);
        assertEquals("fromNewConfig", prop.get());
        ConfigurationManager.getConfigInstance().setProperty("xyz", "new");
        assertEquals("new", lastModified);
    }

}
