package com.netflix.archaius;

import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.SettableConfig;

public interface AppConfig extends PropertyFactory, SettableConfig, CompositeConfig, ConfigLoader {

}
