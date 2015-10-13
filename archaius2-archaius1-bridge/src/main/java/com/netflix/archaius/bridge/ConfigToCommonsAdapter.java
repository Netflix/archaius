package com.netflix.archaius.bridge;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.archaius.Config;

/**
 * Adapter from an Archaius2 configuration to an Apache Commons Configuration.
 * 
 * Note that since Archaius2 treats the Config as immutable setting properties
 * is not allowed.
 * 
 * @author elandau
 */
class ConfigToCommonsAdapter extends AbstractConfiguration {

    private Config config;

    public ConfigToCommonsAdapter(Config config) {
        this.config = config;
    }
    
    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    @Override
    public Object getProperty(String key) {
        return config.getString(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        throw new UnsupportedOperationException("Can't set key '" + key + "'. Config is immutable.");
    }
}
