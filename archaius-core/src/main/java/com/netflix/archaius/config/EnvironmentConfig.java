package com.netflix.archaius.config;

import java.util.Iterator;
import java.util.Map;

public class EnvironmentConfig extends AbstractConfig {

    private static final String DEFAULT_NAME = "ENVIRONMENT";
    
    private final Map<String, String> properties;
    
    public EnvironmentConfig() {
        this(DEFAULT_NAME);
    }

    public EnvironmentConfig(String name) {
        super(name);
        this.properties = System.getenv();
    }

    @Override
    public String getRawString(String key) {
        return properties.get(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }
    
    @Override
    public Iterator<String> getKeys() {
        return properties.keySet().iterator();
    }
}
