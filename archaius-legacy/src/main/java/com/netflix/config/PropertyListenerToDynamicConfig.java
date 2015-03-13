package com.netflix.config;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

import com.netflix.archaius.config.AbstractDynamicConfig;

public class PropertyListenerToDynamicConfig extends AbstractDynamicConfig {

    private DynamicConfiguration config;

    public PropertyListenerToDynamicConfig(String name, DynamicConfiguration config) {
        super(name);
        this.config = config;
        this.config.addConfigurationListener(new ConfigurationListener() {
            @Override
            public void configurationChanged(ConfigurationEvent event) {
                if (!event.isBeforeUpdate()) {
                    switch (event.getType()) {
                    case AbstractConfiguration.EVENT_ADD_PROPERTY:
                    case AbstractConfiguration.EVENT_SET_PROPERTY:
                    case AbstractConfiguration.EVENT_CLEAR_PROPERTY:
                        notifyOnUpdate(event.getPropertyName());
                        break;
                    case AbstractConfiguration.EVENT_CLEAR:
                        notifyOnUpdate();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public boolean containsProperty(String key) {
        return config.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    public String getRawString(String key) {
        // TODO: This could be a list
        return config.getProperty(key).toString();
    }
}
