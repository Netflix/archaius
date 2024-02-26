package com.netflix.archaius.bridge;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.netflix.archaius.api.Config;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class BaseBridgeTest {
    @Singleton
    public static class SomeClient {
        final String fooValue;
        
        @Inject
        public SomeClient(Config config) {
            fooValue = ConfigurationManager.getConfigInstance().getString("app.override.foo", null);
        }
    }
    
    @Test
    public void confirmLegacyOverrideOrderResources() throws IOException, ConfigurationException {
    	AggregatedConfiguration config = (AggregatedConfiguration)ConfigurationManager.getConfigInstance();
    	
    	ConfigurationManager.loadPropertiesFromConfiguration(
    			new PropertiesConfiguration("AbstractConfigurationBridgeTest_libA_legacy.properties"));
    	
        assertTrue(config.getBoolean("libA.legacy.loaded",  false));
        assertEquals("libA", config.getString("lib.legacy.override", null));
        assertTrue(config.getBoolean("libA.legacy.loaded"));
        
        ConfigurationManager.loadPropertiesFromConfiguration(
        		new PropertiesConfiguration("AbstractConfigurationBridgeTest_libB_legacy.properties"));
        assertTrue(config.getBoolean("libB.legacy.loaded", false));
        assertEquals("libA", config.getString("lib.legacy.override", null));
        assertTrue(config.getBoolean("libB.legacy.loaded"));
    }
}
