package com.netflix.config;

public class DynamicBooleanProperty extends PropertyWrapper<Boolean> {
    DynamicBooleanProperty(String propName, boolean defaultValue) {
        super(propName, Boolean.valueOf(defaultValue));
    }
    public boolean get() {
        return prop.getBoolean(defaultValue).booleanValue();
    }
    @Override
    public Boolean getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
