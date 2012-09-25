package com.netflix.config;

import java.util.Set;

public class DynamicStringSetProperty extends DynamicSetProperty<String> {
    public DynamicStringSetProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringSetProperty(String propName, Set<String> defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringSetProperty(String propName, String defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }

    public DynamicStringSetProperty(String propName, Set<String> defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }

    @Override
    protected String from(String value) {
        return value;
    }
}
