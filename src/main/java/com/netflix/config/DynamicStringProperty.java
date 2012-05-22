package com.netflix.config;

public class DynamicStringProperty extends PropertyWrapper<String> {

    DynamicStringProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
        // TODO Auto-generated constructor stub
    }

    public String get() {
        return prop.getString(defaultValue);
    }

    @Override
    public String getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
