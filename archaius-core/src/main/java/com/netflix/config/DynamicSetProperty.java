/**
 * Copyright 2013 Netflix, Inc.
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
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public abstract class DynamicSetProperty<T> {
    private volatile Set<T> values;

    private Set<T> defaultValues;
    
    private DynamicStringProperty delegate;

    private Splitter splitter;

    public static final String DEFAULT_DELIMITER = ",";

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
    	this.defaultValues = defaultValue;
        this.splitter = splitter;
        delegate = DynamicPropertyFactory.getInstance().getStringProperty(propName, null);
        load();
        delegate.addCallback(new Runnable() {
            @Override
            public void run() {
                propertyChangedInternal();
            }
        });    	
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

    private Set<String> split(String value) {    	    	
    	return Sets.newHashSet(splitter.split(Strings.nullToEmpty(value)));
    }
    
    protected Set<T> transform(Set<String> stringValues) {
        Set<T> set = new HashSet<T>(stringValues.size());
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
    public long getChangedTimestamp() {
        return delegate.getChangedTimestamp();
    }

    /**
     * Add the callback to be triggered when the value of the property is changed
     *
     */
    public void addCallback(Runnable callback) {
        if (callback != null) delegate.addCallback(callback);
    }

    /**
     * Construct the generic type from string.
     * 
     */
    protected abstract T from(String value);

    /**
     * Getter for the property name
     * @return
     */
    public String getName(){
        return delegate.getName();
    }
}
