package com.netflix.archaius.api;

import com.netflix.archaius.api.annotations.ConfigurationSource;

/**
 * API for loading configurations into the libraries layer
 */
public interface LibrariesConfig {
    void load(ConfigurationSource source);
}
