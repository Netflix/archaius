/*
 *
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.netflix.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * This class delegates to a regex (default is comma) delimited dynamic string property and 
 * returns a dynamic list of a generic type which is transformed from string.
 * 
 * @author awang
 */
public abstract class DynamicListProperty<T> {
    private volatile List<T> values;

    private List<T> defaultValues;
    
    private DynamicStringProperty delegate;

    private Splitter splitter;

    public static final String DEFAULT_DELIMITER = ",";

    /**
     * Create the dynamic list property using default delimiter regex ",". The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * list value will be transformed from list of strings after splitting. If defaultValue string is
     * null, the default list value will be an empty list.
     */
    public DynamicListProperty(String propName, String defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }

    /**
     * Create the dynamic list property. The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * list value will be transformed from list of strings after splitting. If defaultValue string is
     * null, the default list value will be an empty list.
     */
    public DynamicListProperty(String propName, String defaultValue, String delimiterRegex) {
    	this.splitter = Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults();
    	setup(propName, transform(split(defaultValue)), splitter);
    }

    public DynamicListProperty(String propName, List<T> defaultValue) {
        this(propName, defaultValue, DEFAULT_DELIMITER);
    }

    /**
     * Create the dynamic list property. The guava Splitter used is created as
     * <code>Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults()</code>. The default
     * list value will be taken from the passed in list argument.
     */
    public DynamicListProperty(String propName, List<T> defaultValue, String delimiterRegex) {
    	setup(propName, defaultValue, delimiterRegex);
    }

    /**
     * Create the dynamic list property using the splitter and default list value passed in 
     * from the arguments.
     */
    public DynamicListProperty(String propName, List<T> defaultValue, Splitter splitter) {
    	setup(propName, defaultValue, splitter);
    }

    
    private void setup(String propName, List<T> defaultValue, String delimiterRegex) {
        setup(propName, defaultValue, Splitter.onPattern(delimiterRegex).omitEmptyStrings().trimResults());
    }

    private void setup(String propName, List<T> defaultValue, Splitter splitter) {
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
     * Get the list type from the underlying dynamic string property. If the property is undefined, this method
     * returns the default list value.   
     */
    public List<T> get() {
        return values;
    }

    private List<String> split(String value) {    	
    	return Lists.newArrayList(splitter.split(Strings.nullToEmpty(value)));
    }
    
    protected List<T> transform(List<String> stringValues) {
        List<T> list = new ArrayList<T>(stringValues.size());
        for (String s : stringValues) {
            list.add(from(s));
        }
        return Collections.unmodifiableList(list);	
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
}
