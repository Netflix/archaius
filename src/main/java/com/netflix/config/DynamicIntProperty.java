package com.netflix.config;

import com.netflix.config.PropertyWrapper;

public class DynamicIntProperty extends PropertyWrapper<Integer> {
    DynamicIntProperty(String propName, int defaultValue) {
        super(propName, Integer.valueOf(defaultValue));
    }
        
    public int get() {
        return prop.getInteger(defaultValue).intValue();
    }

    @Override
    public Integer getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
