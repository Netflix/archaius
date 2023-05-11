package com.netflix.archaius.api.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Layer;
import com.netflix.archaius.api.exceptions.ConfigException;

import java.util.Collection;
import java.util.Optional;

/**
 * Composite Config where the override order is driven by Layer keys.
 */
public interface LayeredConfig extends Config {
    static interface LayeredVisitor<T> extends Visitor<T> {
        /**
         * Visit a Config at the specified layer.  visitConfig is called in override order
         *
         * @param layer
         * @param child
         * @return
         */
        T visitConfig(Layer layer, Config config);
    }
    
    /**
     * Add a Config at the specified Layer.
     * 
     * <p>
     * This will trigger an onConfigUpdated event.
     *
     * @param layer
     * @param child
     */
    void addConfig(Layer layer, Config config);
    
    void addConfig(Layer layer, Config config, int position);
    
    Optional<Config> removeConfig(Layer layer, String name);
    
    /**
     * Return all property sources at a layer
     * @param layer
     * @return Immutable list of all property sources at the specified layer.
     */
    Collection<Config> getConfigsAtLayer(Layer layer);
}