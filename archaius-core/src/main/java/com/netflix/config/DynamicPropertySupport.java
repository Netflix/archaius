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

/**
 * The interface that defines the contract between DynamicProperty and its
 * underlying support system.
 * <p>
 * In most cases, it will be much easier to use Apache Commons Configuration to 
 * support {@link DynamicProperty}. However, this interface makes it possible for
 * {@link DynamicProperty} to work without relying on Apache Commons Configuration.
 * 
 * @author kranganathan
 *
 */
public interface DynamicPropertySupport {

    /**
     * Get the string value of a given property. The string value will be further 
     * cached and parsed into specific type for {@link DynamicProperty}.
     * 
     * @param propName The name of the property
     * @return The String value of the property 
     */
    String getString(String propName);

    /**
     * Add the property change listener. This is necessary for the {@link DynamicProperty} to
     * receive callback once a property is updated in the underlying {@link DynamicPropertySupport}
     * 
     * @param expandedPropertyListener Listener to be added to {@link DynamicPropertySupport}
     */
    void addConfigurationListener(PropertyListener expandedPropertyListener);
}
