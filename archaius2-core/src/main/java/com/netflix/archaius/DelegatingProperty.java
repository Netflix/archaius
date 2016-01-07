package com.netflix.archaius;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;

public abstract class DelegatingProperty<T> implements Property<T> {

    protected Property<T> delegate;

    public DelegatingProperty(Property<T> delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void addListener(PropertyListener<T> listener) {
        delegate.addListener(listener);
    }
    
    @Override
    public void removeListener(PropertyListener<T> listener) {
        delegate.removeListener(listener);
    }
    
    @Override
    public String getKey() {
        return delegate.getKey();
    }
}
