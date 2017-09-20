package com.netflix.archaius.bridge;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.netflix.archaius.api.Config;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Ignore
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
    	
        Assert.assertTrue(config.getBoolean("libA.legacy.loaded",  false));
        Assert.assertEquals("libA", config.getString("lib.legacy.override", null));
    	Assert.assertTrue(config.getBoolean("libA.legacy.loaded"));
        
        ConfigurationManager.loadPropertiesFromConfiguration(
        		new PropertiesConfiguration("AbstractConfigurationBridgeTest_libB_legacy.properties"));
        Assert.assertTrue(config.getBoolean("libB.legacy.loaded", false));
        Assert.assertEquals("libA", config.getString("lib.legacy.override", null));
    	Assert.assertTrue(config.getBoolean("libB.legacy.loaded"));

    }

}
