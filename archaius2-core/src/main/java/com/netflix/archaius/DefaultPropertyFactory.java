package com.netflix.archaius;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.PropertyContainer;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.property.DefaultPropertyContainer;
import com.netflix.archaius.property.ListenerManager;

public final class DefaultPropertyFactory implements PropertyFactory {
    /**
     * Create a Property factory that is attached to a specific config
     * @param config
     * @return
     */
    public static DefaultPropertyFactory from(final Config config) {
        return new DefaultPropertyFactory(config);
    }

    /**
     * Config from which properties are retrieved.  Config may be a composite.
     */
    private final Config config;
    
    /**
     * Cache of properties so PropertyContainer may be re-used
     */
    private final ConcurrentMap<String, PropertyContainer> cache = new ConcurrentHashMap<String, PropertyContainer>();
    
    /**
     * Monotonically incrementing version number whenever a change in the Config
     * is identified.  This version is used as a global dirty flag indicating that
     * properties should be updated when fetched next.
     */
    private final AtomicInteger version = new AtomicInteger();
    
    /**
     * Array of all active callbacks.  ListenerWrapper#update will be called for any
     * change in config.  
     */
    private final ListenerManager listeners = new ListenerManager();
    
    public DefaultPropertyFactory(Config config) {
        this.config = config;
        this.config.addListener(new ConfigListener() {
            @Override
            public void onConfigAdded(Config config) {
                invalidate();
            }

            @Override
            public void onConfigRemoved(Config config) {
                invalidate();
            }

            @Override
            public void onConfigUpdated(Config config) {
                invalidate();
            }

            @Override
            public void onError(Throwable error, Config config) {
                // TODO:
            }
        });
    }

    @Override
    public PropertyContainer getProperty(String propName) {
        PropertyContainer container = cache.get(propName);
        if (container == null) {
            container = new DefaultPropertyContainer(propName, config, version, listeners);
            PropertyContainer existing = cache.putIfAbsent(propName, container);
            if (existing != null) {
                return existing;
            }
        }
        
        return container;
    }
    
    public void invalidate() {
        // Incrementing the version will cause all PropertyContainer instances to invalidate their
        // cache on the next call to get
        version.incrementAndGet();
        
        // We expect a small set of callbacks and invoke all of them whenever there is any change
        // in the configuration regardless of change. The blanket update is done since we don't track
        // a dependency graph of replacements.
        listeners.updateAll();
    }
}
