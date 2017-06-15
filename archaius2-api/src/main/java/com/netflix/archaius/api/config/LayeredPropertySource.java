package com.netflix.archaius.api.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Config.Visitor;
import com.netflix.archaius.api.Layer;
import com.netflix.archaius.api.PropertySource;
import com.netflix.archaius.api.exceptions.ConfigException;

public interface LayeredPropertySource extends PropertySource {
    static interface LayeredVisitor<T> extends Visitor<T> {
        /**
         * Visit a child of the configuration
         *
         * @param layer
         * @param child
         * @return
         */
        T visitChild(Layer layer, Config child);
    }
    
    /**
     * Add a named configuration.  The newly added configuration takes precedence over all
     * previously added configurations.  Duplicate configurations are not allowed
     * <p>
     * This will trigger an onConfigAdded event.
     *
     * @param layer
     * @param child
     * @throws ConfigException
     */
    boolean addPropertySource(Layer layer, PropertySource propertySource) throws ConfigException;
}
