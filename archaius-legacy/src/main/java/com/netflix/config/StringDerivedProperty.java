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

import com.google.common.base.Function;

/**
 * A property wrapper that can be used to derive any type of data as property value 
 * from string format.
 * 
 * @author awang
 *
 * @param <T> Type of the property value
 */
public class StringDerivedProperty<T> extends PropertyWrapper<T> {
    protected final Function<String, T> decoder;
    
    private volatile T derivedValue;
    
    /**
     * Create an instance of the property wrapper.
     *  
     * @param decoder the function used to parse the string format into the desired data type.
     */
    public StringDerivedProperty(String propName, T defaultValue, Function<String, T> decoder) {
        super(propName, defaultValue);
        this.decoder = decoder;
        propertyChangedInternal();
    }

    private final void propertyChangedInternal() {
        String stringValue = prop.getString();
        if (stringValue == null) {
            derivedValue = defaultValue;
        } else {
            derivedValue = decoder.apply(stringValue);
        }        
    }
    
    @Override
    protected final void propertyChanged() {
        propertyChangedInternal();
        propertyChanged(getValue());
    }
    
    @Override
    public T getValue() {
        return derivedValue;
    }
}
