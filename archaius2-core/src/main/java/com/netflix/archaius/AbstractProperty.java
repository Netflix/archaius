package com.netflix.archaius;

import com.netflix.archaius.api.ListenerSubscription;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;

public abstract class AbstractProperty<T> implements Property<T> {

    private final String key;
    
    public AbstractProperty(String key) {
        this.key = key;
    }
    
    @Override
    public ListenerSubscription addListener(PropertyListener<T> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey() {
        return key;
    }
}
