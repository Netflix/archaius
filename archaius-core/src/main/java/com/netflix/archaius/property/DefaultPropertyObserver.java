package com.netflix.archaius.property;

import com.netflix.archaius.PropertyObserver;

public class DefaultPropertyObserver<T> implements PropertyObserver<T> {
    @Override
    public void onChange(T value) {
    }
    
    @Override
    public void onError(Throwable error) {
    }
}
