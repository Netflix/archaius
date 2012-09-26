package com.netflix.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DynamicSetProperty<T> {
    private volatile Set<T> values;
    
    private final DynamicStringProperty delegate;
    
    private final String delimiter;
        
    public DynamicSetProperty(String propName, String defaultValue) {
        this(propName, defaultValue, DynamicListProperty.DEFAULT_DELIMITER);
    }
    
    public DynamicSetProperty(String propName, String defaultValue, String delimiterRegex) {
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

    public DynamicSetProperty(String propName, Set<T> defaultValue, String delimiterRegex) {
        this(propName, (String) null, delimiterRegex);
        if (values == null && defaultValue != null) {
            values = Collections.unmodifiableSet(defaultValue);
        }
    }

    public DynamicSetProperty(String propName, Set<T> defaultValue) {
        this(propName, defaultValue, DynamicListProperty.DEFAULT_DELIMITER);
    }

    private void propertyChangedInternal() {
        load();
        propertyChanged();        
    }
    
    protected void propertyChanged() {
    }

    public Set<T> get() {
        return values;
    }

    protected void load() {
        if (delegate.get() == null) {
            return;
        }
        final List<String> strings = Arrays.asList(delegate.get().split(delimiter));
        Set<T> set = new HashSet<T>(strings.size());
        for (String s : strings) {
            set.add(from(s));
        }
        values = Collections.unmodifiableSet(set);
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
