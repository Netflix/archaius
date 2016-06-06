package com.netflix.archaius.test;

import java.util.Iterator;
import java.util.Properties;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;

/**
 * Implementation of {@link DefaultCompositeConfig} and {@link SettableConfig}
 * for use in testing utilities. 
 */
public class TestCompositeConfig extends DefaultCompositeConfig implements SettableConfig {

    private static final String CLASS_LEVEL_LAYER_NAME =    "CLASS_LEVEL_TEST_OVERRIDES";
    private static final String METHOD_LEVEL_LAYER_NAME =   "METHOD_LEVEL_TEST_OVERRIDES";
    private static final String RUNTIME_LAYER_NAME =        "RUNTIME";
    
    public TestCompositeConfig(SettableConfig classLevelOverrides, SettableConfig methodLevelOverrides) {
        try {
            addConfig(RUNTIME_LAYER_NAME, new DefaultSettableConfig());
            addConfig(METHOD_LEVEL_LAYER_NAME, methodLevelOverrides);
            addConfig(CLASS_LEVEL_LAYER_NAME, classLevelOverrides);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void resetForTest() {
        clear((SettableConfig) getConfig(METHOD_LEVEL_LAYER_NAME));
        clear((SettableConfig) getConfig(RUNTIME_LAYER_NAME));
    }
    
    private void clear(SettableConfig config) {
        Iterator<String> keys = config.getKeys();
        while(keys.hasNext()) {
            config.clearProperty(keys.next());
        }
    }

    @Override
    public void setProperties(Config config) {
        ((SettableConfig) getConfig(RUNTIME_LAYER_NAME)).setProperties(config);
    }

    @Override
    public void setProperties(Properties properties) {
        ((SettableConfig) getConfig(RUNTIME_LAYER_NAME)).setProperties(properties);
    }

    @Override
    public <T> void setProperty(String propName, T propValue) {
        ((SettableConfig) getConfig(RUNTIME_LAYER_NAME)).setProperty(propName, propValue);
    }

    @Override
    public void clearProperty(String propName) {
        ((SettableConfig) getConfig(RUNTIME_LAYER_NAME)).clearProperty(propName);
    }
}
