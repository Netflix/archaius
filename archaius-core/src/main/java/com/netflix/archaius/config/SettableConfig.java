/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius.config;

import java.util.Properties;

import com.netflix.archaius.Config;

/**
 * SPI for a config that may be set from code.
 * 
 * @author elandau
 *
 */
public interface SettableConfig extends Config {
    /**
     * Set a bunch of proeprties
     * @param properties
     */
    void setProperties(Properties properties);
    
    /**
     * Set a single property 
     * @param propName
     * @param propValue
     */
    <T> void setProperty(String propName, T propValue);
    
    /**
     * Clear a property.  Note that the when part of a CompositeConfig only the property
     * tracked by the settable config will be cleared and a value for the propertyName
     * may exist in a different child config
     * @param propName
     */
    void clearProperty(String propName);
}
