package com.netflix.archaius;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;

public abstract class AbstractProperty<T> implements Property<T> {

    private final String key;
    
    public AbstractProperty(String key) {
        this.key = key;
    }
    
    @Override
    public void unsubscribe() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property<T> addListener(PropertyListener<T> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(PropertyListener<T> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey() {
        return key;
    }
}
