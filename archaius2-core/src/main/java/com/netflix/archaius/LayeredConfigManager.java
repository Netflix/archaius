package com.netflix.archaius;

import com.netflix.archaius.api.Layer;
import com.netflix.archaius.api.PropertySource;
import com.netflix.archaius.api.config.LayeredPropertySource;
import com.netflix.archaius.api.exceptions.ConfigException;

public class LayeredConfigManager implements LayeredPropertySource {
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addPropertySource(Layer layer, PropertySource propertySource) throws ConfigException {
        // TODO Auto-generated method stub
        return false;
    }
}
