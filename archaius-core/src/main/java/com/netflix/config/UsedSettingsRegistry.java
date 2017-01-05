package com.netflix.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds settings that were actually requested by application.
 */
public class UsedSettingsRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final ConcurrentMap<String, Object> settings = new ConcurrentHashMap<String, Object>();
    private static final UsedSettingsRegistry instance = new UsedSettingsRegistry();

    private UsedSettingsRegistry() {
    }

    public static UsedSettingsRegistry instance() {
        return instance;
    }

    public void add(String key, Object value) {
        if (key == null || value == null) {
            logger.trace("NOT Adding setting key: {} value: {} (null not permitted)", key, value);
            return;
        }
        logger.trace("Adding setting key: {} value: {}", key, value);
        settings.put(key, value);
    }

    public Map<String, Object> getSettings() {
        logger.trace("Get all settings: {}", settings);
        return Collections.unmodifiableMap(settings);
    }
}
