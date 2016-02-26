/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class DynamicSetProperty<T> implements Property<Set<T>> {
    private volatile Set<T> values;

    private Set<T> defaultValues;
    
    private DynamicStringProperty delegate;

    private Splitter splitter;

    public static final String DEFAULT_DELIMITER = ",";

    private final List<Runnable> callbackList = Lists.newArrayList();

    /**
     * Create the dynamic set property using default delimiter regex ",". The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * set value will be transformed from set of strings after splitting. If defaultValue string is
     * null, the default set value will be an empty set.
     */
    public DynamicSetProperty(String propName, String defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }

    /**
     * Create the dynamic set property. The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * set value will be transformed from set of strings after splitting. If defaultValue string is
     * null, the default set value will be an empty set.
     */
    public DynamicSetProperty(String propName, String defaultValue, String delimiterRegex) {
        this.splitter = Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults();
        setup(propName, transform(split(defaultValue)), splitter);
    }

    public DynamicSetProperty(String propName, Set<T> defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }

    /**
     * Create the dynamic set property. The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * set value will be taken from the passed in set argument.
     */
    public DynamicSetProperty(String propName, Set<T> defaultValue, String delimiterRegex) {
        setup(propName, defaultValue, delimiterRegex);
    }

    /**
     * Create the dynamic set property using the splitter and default set value passed in 
     * from the arguments.
     */
    public DynamicSetProperty(String propName, Set<T> defaultValue, Splitter splitter) {
        setup(propName, defaultValue, splitter);
    }

    
    private void setup(String propName, Set<T> defaultValue, String delimiterRegex) {
        setup(propName, defaultValue, Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults());
    }

    private void setup(String propName, Set<T> defaultValue, Splitter splitter) {
        // Make a defensive copy of the default value. Would prefer to use an ImmutableSet, but that
        // does not allow for null values in the Set.
        this.defaultValues = (defaultValue == null ? null : 
            Collections.unmodifiableSet(new LinkedHashSet<T>(defaultValue)));
        
        this.splitter = splitter;
        delegate = DynamicPropertyFactory.getInstance().getStringProperty(propName, null);
        load();
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                propertyChangedInternal();
            }
        };
        delegate.addCallback(callback);
        callbackList.add(callback);
    }

    private void propertyChangedInternal() {
        load();
        propertyChanged();
    }

    /**
     * A method invoked when the underlying string property is changed. Default implementation does nothing. 
     * Subclass can override this method 
     * to receive callback. 
     */
    protected void propertyChanged() {
    }

    /**
     * Get the set type from the underlying dynamic string property. If the property is undefined, this method
     * returns the default set value.   
     */
    public Set<T> get() {
        return values;
    }
    
    @Override
    public Set<T> getValue() {
        return get();
    }
    
    @Override
    public Set<T> getDefaultValue() {
        return defaultValues;
    }

    private Set<String> split(String value) {                
        return Sets.newLinkedHashSet(splitter.split(Strings.nullToEmpty(value)));
    }
    
    protected Set<T> transform(Set<String> stringValues) {
        Set<T> set = new LinkedHashSet<T>(stringValues.size());
        for (String s : stringValues) {
            set.add(from(s));
        }
        return Collections.unmodifiableSet(set);    
    }
    
    
    protected void load() {
        if (delegate.get() == null) {
            values = defaultValues;
        } else {
            values = transform(split(delegate.get()));
        }
    }

    /**
     * Gets the time (in milliseconds past the epoch) when the property
     * was last set/changed.
     */
    @Override
    public long getChangedTimestamp() {
        return delegate.getChangedTimestamp();
    }

    /**
     * Add the callback to be triggered when the value of the property is changed
     *
     */
    @Override
    public void addCallback(Runnable callback) {
        if (callback != null) {
            delegate.addCallback(callback);
            callbackList.add(callback);
        }
    }

    /**
     * Remove all callbacks registered through this instance of property
     */
    @Override
    public void removeAllCallbacks() {
        for (Runnable callback: callbackList) {
            delegate.prop.removeCallback(callback);
        }
    }

    /**
     * Construct the generic type from string.
     * 
     */
    protected abstract T from(String value);

    /**
     * Getter for the property name
     */
    @Override
    public String getName(){
        return delegate.getName();
    }
}
