package com.netflix.config;

import java.util.List;

public class DynamicStringListProperty extends DynamicListProperty<String> {
    public DynamicStringListProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringListProperty(String propName, List<String> defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringListProperty(String propName, String defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }

    public DynamicStringListProperty(String propName, List<String> defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }
    
    @Override
    protected String from(String value) {
        return value;
    }
    
    public static void main(String[] args) {
        DynamicStringListProperty prop = new DynamicStringListProperty("abc", "abc, de", ",\\s+");
        System.out.println(prop.get());
    }
}
