package com.netflix.config;

import java.util.Map;

public class DynamicStringMapProperty extends DynamicMapProperty<String, String> {

    public DynamicStringMapProperty(String propName, String defaultValue,
            String delimiter) {
        super(propName, defaultValue, delimiter);
    }

    public DynamicStringMapProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }
    
    public DynamicStringMapProperty(String propName, Map<String, String> defaultValue,
            String delimiter) {
        super(propName, defaultValue, delimiter);
    }

    public DynamicStringMapProperty(String propName, Map<String, String> defaultValue) {
        super(propName, defaultValue);
    }


    @Override
    protected String getKey(String key) {
        return key;
    }

    @Override
    protected String getValue(String value) {
        return value;
    }
    
    public static void main(String[] args) {
        DynamicStringMapProperty prop = new DynamicStringMapProperty("test", "a=abc,b=xyz");
        System.out.println(prop.getMap());
    }
}
