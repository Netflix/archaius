package com.netflix.archaius.property;

import com.netflix.archaius.PropertyContainer;
import com.netflix.archaius.PropertyFactory;

public class PrefixedObservablePropertyFactory implements PropertyFactory {

    private final String prefix;
    private final PropertyFactory delegate;
    
    public PrefixedObservablePropertyFactory(String prefix, PropertyFactory delegate) {
        this.prefix = !prefix.isEmpty() && !prefix.endsWith(".") 
                    ? prefix + "." 
                    : prefix;
        this.delegate = delegate;
    }
    
    @Override
    public PropertyContainer connectProperty(String propName) {
        return delegate.connectProperty(prefix + propName);
    }
}
