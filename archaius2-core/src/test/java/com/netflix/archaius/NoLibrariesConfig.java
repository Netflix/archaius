package com.netflix.archaius;

import com.netflix.archaius.api.LibrariesConfig;
import com.netflix.archaius.api.annotations.ConfigurationSource;

public class NoLibrariesConfig implements LibrariesConfig {
    @Override
    public void load(ConfigurationSource source) {
    }
}

