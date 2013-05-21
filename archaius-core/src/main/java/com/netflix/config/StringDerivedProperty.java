package com.netflix.config;

import com.google.common.base.Function;

public class StringDerivedProperty<T> extends PropertyWrapper<T> {
    protected final Function<String, T> deriveFunction;
    
    private volatile T derivedValue;
    
    public StringDerivedProperty(String propName, T defaultValue, Function<String, T> deriveFunction) {
        super(propName, defaultValue);
        this.deriveFunction = deriveFunction;
        propertyChangedInternal();
    }

    private final void propertyChangedInternal() {
        String stringValue = prop.getString();
        if (stringValue == null) {
            derivedValue = defaultValue;
        } else {
            derivedValue = deriveFunction.apply(stringValue);
        }        
    }
    
    @Override
    protected final void propertyChanged() {
        propertyChangedInternal();
        propertyChanged(getValue());
    }
    
    @Override
    public T getValue() {
        return derivedValue;
    }
}
