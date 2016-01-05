package com.netflix.archaius;

import com.netflix.archaius.api.ListenerSubscription;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;

public abstract class DelegatingProperty<T> implements Property<T> {

    protected Property<T> delegate;

    public DelegatingProperty(Property<T> delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public ListenerSubscription addListener(PropertyListener<T> listener) {
        return delegate.addListener(listener);
    }
    @Override
    public String getKey() {
        return delegate.getKey();
    }
}
