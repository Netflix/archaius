package com.netflix.config;

import java.util.Map;

public class DynamicStringMapProperty extends DynamicMapProperty<String, String> {

    public DynamicStringMapProperty(String propName, String defaultValue,
            String mapEntryDelimiterRegex) {
        super(propName, defaultValue, mapEntryDelimiterRegex);
    }

    public DynamicStringMapProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }
    
    public DynamicStringMapProperty(String propName, Map<String, String> defaultValue,
            String mapEntryDelimiterRegex) {
        super(propName, defaultValue, mapEntryDelimiterRegex);
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
}
