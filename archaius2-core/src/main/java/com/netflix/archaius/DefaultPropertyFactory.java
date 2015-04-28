package com.netflix.archaius;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.netflix.archaius.property.DefaultPropertyContainer;

public class DefaultPropertyFactory implements PropertyFactory, ConfigListener {
    /**
     * Create a Property factory that is attached to a specific config
     * @param config
     * @return
     */
    public static DefaultPropertyFactory from(final Config config) {
        return new DefaultPropertyFactory(config);
    }

    private final Config config;
    private final ConcurrentMap<String, PropertyContainer> registry = new ConcurrentHashMap<String, PropertyContainer>();
    
    public DefaultPropertyFactory(Config config) {
        this.config = config;
        this.config.addListener(this);
    }

    @Override
    public PropertyContainer getProperty(String propName) {
        PropertyContainer container = registry.get(propName);
        if (container == null) {
            container = new DefaultPropertyContainer(propName, config);
            PropertyContainer existing = registry.putIfAbsent(propName, container);
            if (existing != null) {
                return existing;
            }
        }
        
        return container;
    }
    
    @Override
    public void onConfigAdded(Config config) {
        invalidate();
    }

    @Override
    public void onConfigRemoved(Config config) {
        invalidate();
    }

    @Override
    public void onError(Throwable error, Config config) {
        // TODO
    }

    @Override
    public void onConfigUpdated(Config config) {
        invalidate();
    }

    public void invalidate() {
        for (PropertyContainer prop : registry.values()) {
            prop.update();
        }
    }
}
