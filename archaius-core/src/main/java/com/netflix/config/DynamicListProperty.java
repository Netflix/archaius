package com.netflix.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class DynamicListProperty<T> {
    private volatile List<T> values;
    
    private final DynamicStringProperty delegate;
    
    private final String delimiter;

    public static final String DEFAULT_DELIMITER = ",";
    
    public DynamicListProperty(String propName, String defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }
    
    public DynamicListProperty(String propName, String defaultValue, String delimiterRegex) {
        delegate = DynamicPropertyFactory.getInstance().getStringProperty(propName, defaultValue);
        delimiter = delimiterRegex;
        load();
        delegate.addCallback(new Runnable() {
            @Override
            public void run() {
                propertyChangedInternal();
            }
        });
    }

    public DynamicListProperty(String propName, List<T> defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }

    public DynamicListProperty(String propName, List<T> defaultValue, String delimiterRegex) {
        this(propName, (String) null, delimiterRegex);
        if (values == null && defaultValue != null) {
            values = Collections.unmodifiableList(defaultValue);
        }
    }

    private void propertyChangedInternal() {
        load();
        propertyChanged();
    }
    
    protected void propertyChanged() {
    }

    public List<T> get() {
        return values;
    }

    protected void load() {
        if (delegate.get() == null) {
            return;
        }
        final List<String> strings = Arrays.asList(delegate.get().split(delimiter));
        List<T> list = new ArrayList<T>(strings.size());
        for (String s : strings) {
            list.add(from(s));
        }
        values = Collections.unmodifiableList(list);
    }  
    
    /**
     * Gets the time (in milliseconds past the epoch) when the property
     * was last set/changed.
     */
    public long getChangedTimestamp() {
        return delegate.getChangedTimestamp();
    }
    
    /**
     * Add the callback to be triggered when the value of the property is changed
     * 
     * @param callback
     */
    public void addCallback(Runnable callback) {
        delegate.addCallback(callback);
    }
    
    protected abstract T from(String value);
}
