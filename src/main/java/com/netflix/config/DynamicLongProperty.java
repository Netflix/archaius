package com.netflix.config;

public class DynamicLongProperty extends PropertyWrapper<Long> {
    DynamicLongProperty(String propName, long defaultValue) {
        super(propName, Long.valueOf(defaultValue));
    }
        
    public long get() {
        return prop.getLong(defaultValue).longValue();
    }

    @Override
    public Long getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
