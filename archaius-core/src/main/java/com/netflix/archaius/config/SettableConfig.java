package com.netflix.archaius.config;

import java.util.Properties;

import com.netflix.archaius.Config;

public interface SettableConfig extends Config {
    void setProperties(Properties properties);
    void setProperty(String propName, Object propValue);
    void clearProperty(String propName);
}
