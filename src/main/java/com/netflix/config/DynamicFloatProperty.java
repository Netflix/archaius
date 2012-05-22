package com.netflix.config;

public class DynamicFloatProperty extends PropertyWrapper<Float> {
    DynamicFloatProperty(String propName, float defaultValue) {
        super(propName, Float.valueOf(defaultValue));
    }
    public float get() {
        return prop.getFloat(defaultValue).floatValue();
    }
    @Override
    public Float getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
