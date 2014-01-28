/*
 *
 * Copyright 2014 Netflix, Inc.
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

/**
 * Base interface for Archaius properties. Provides common methods across all
 * property implementations.
 * 
 * @param <T> The value type of the property
 */
public interface Property<T> {

    /**
     * Get the latest value for the given property
     * 
     * @return the latest property value
     */
    T getValue();
    
    /**
     * Get the default property value specified at creation time
     * 
     * @return the default property value
     */
    T getDefaultValue();

    /**
     * Get the name of the property
     * 
     * @return the property name
     */
    String getName();

    /**
     * Gets the time (in milliseconds past the epoch) when the property was last
     * set/changed.
     */
    long getChangedTimestamp();

    /**
     * Add the callback to be triggered when the value of the property is
     * changed
     * 
     * @param callback
     */
    void addCallback(Runnable callback);

    /**
     * remove all callbacks registered through the instance of property
     */
    void removeAllCallbacks();

}
