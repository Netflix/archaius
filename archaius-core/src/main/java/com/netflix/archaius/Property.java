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
package com.netflix.archaius;

import java.util.concurrent.TimeUnit;

/**
 * API to access latest cached value for a Property.  A Property is created from a PropertyFactory
 * that is normally bound to a top level configuration object such ask {@link AppConfig}.  Through
 * a Property its also possible to receive a stream of property update notifications.
 * 
 * {@code 
 * class MyService {
 *     private final Property<String> prop;
 *     
 *     MyService(PropertyFactroy config) {
 *        prop = config.connectProperty("foo.prop").asString("defaultValue");
 *     }
 *     
 *     void doSomething() {
 *         // Will print out the most up to date value for the property
 *         System.out.println(prop.get());
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
     * @return  Most recent value for the property
     */
    T get(T defaultValue);
    
    /**
     * Get the last time the property was updated
     * @param units
     * @return
     */
    long getLastUpdateTime(TimeUnit units);
    
    /**
     * Unsubscribe from property value update notifications.  The property object cannot be resubscribed.
     */
    void unsubscribe();

    Property<T> addObserver(PropertyObserver<T> observer);

    void removeObserver(PropertyObserver<T> observer);
}
