package com.netflix.config;

public class DynamicDoubleProperty extends PropertyWrapper<Double> {
    DynamicDoubleProperty(String propName, double defaultValue) {
        super(propName, Double.valueOf(defaultValue));
    }
        
    public double get() {
        return prop.getDouble(defaultValue).doubleValue();
    }

    @Override
    public Double getValue() {
        return get();
    }
}
