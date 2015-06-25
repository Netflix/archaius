package com.netflix.archaius;

import java.util.concurrent.TimeUnit;

public class ImmutableProperty<T> implements Property<T> {

    private final T value;
    private final String name;
    
    public ImmutableProperty(String name, T value) {
        this.value = value;
        this.name = name;
    }
    
    @Override
    public T get() {
        return value;
    }

    @Override
    public long getLastUpdateTime(TimeUnit units) {
        return 0;
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public Property<T> addListener(PropertyListener<T> listener) {
        return null;
    }

    @Override
    public void removeListener(PropertyListener<T> listener) {
    }

    @Override
    public String getKey() {
        return name;
    }
}
