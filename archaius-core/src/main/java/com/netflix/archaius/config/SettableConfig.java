package com.netflix.archaius.config;

import java.util.Properties;

import com.netflix.archaius.Config;

public interface SettableConfig extends Config {
    void setProperties(Properties properties);
    <T> void setProperty(String propName, T propValue);
    void clearProperty(String propName);
}
