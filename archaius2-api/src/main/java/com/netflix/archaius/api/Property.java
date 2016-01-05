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
package com.netflix.archaius.api;

/**
 * API to access latest cached value for a Property.  A Property is created from a PropertyFactory
 * that is normally bound to a top level configuration.  
 * 
 * {@code 
 * class MyService {
 *     private final Property<String> prop;
 *     
 *     MyService(PropertyFactory factory) {
 *        prop = factory.getProperty("foo.prop").asString("defaultValue");
 *     }
 *     
 *     public void doSomething() {
 *         String currentValue = prop.get();
 *     }
 * }
 * }
 * 
 * TODO: Chain properties
 * TODO: Property validator
 * 
 * @author elandau
 *
 * @param <T>
 */
public interface Property<T> {
    /**
     * Return the most recent value of the property.  
     * 
     * @return  Most recent value for the property
     */
    T get();

    /**
     * Add a listener that will be called whenever the property value changes
     * @param listener
     * @return
     */
    ListenerSubscription addListener(PropertyListener<T> listener);

    /**
     * @return Key or path to the property 
     */
    String getKey();
}
