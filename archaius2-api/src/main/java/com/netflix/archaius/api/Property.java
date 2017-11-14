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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * API for composeable property access with optional chaining with default value support
 * as well as change notification.
 * 
 * A {@link PropertyFactory} implementation normally implements some level of caching
 * to reduce the overhead of interpolating and converting values.
 * 
 * {@code 
 * class MyService {
 *     private final Property<String> prop;
 *     
 *     MyService(PropertyFactory factory) {
 *        prop = factory.getProperty("foo.prop").orElse("defaultValue");
 *     }
 *     
 *     public void doSomething() {
 *         String currentValue = prop.get();
 *     }
 * }
 * }
 * 
 * @param <T>
 */
public interface Property<T> extends Supplier<T> {
    /**
     * Token returned when calling onChange through which change notification can be
     * unsubscribed.  
     */
    interface Subscription {
        void unsubscribe();
    }
    
    /**
     * Return the most recent value of the property.  
     * 
     * @return  Most recent value for the property
     */
    T get();

    /**
     * Add a listener that will be called whenever the property value changes
     * @param listener
     */
    @Deprecated
    default void addListener(PropertyListener<T> listener) {
        onChange(new Consumer<T>() {
            @Override
            public void accept(T t) {
                listener.accept(t);
            }
        });
    }

    /**
     * Remove a listener previously registered by calling addListener
     * @param listener
     */
    @Deprecated
    default void removeListener(PropertyListener<T> listener) {}
    
    /**
     * Register for notification whenever the property value changes.
     * {@link Property#onChange(Consumer)} should be called last when chaining properties
     * since the notification only applies to the state of the chained property
     * up until this point. Changes to subsequent Property objects returned from {@link Property#orElse} 
     * or {@link Property#map(Function)} will not trigger calls to this consumer.

     * @param consumer
     * @return Subscription that may be unsubscribed to no longer get change notifications
     */
    default Subscription onChange(Consumer<T> consumer) {
        PropertyListener<T> listener = new PropertyListener<T>() {
            @Override
            public void onChange(T value) {
                consumer.accept(value);
            }
        };
        
        addListener(listener);
        return () -> removeListener(listener);
    }
    
    /**
     * Create a new Property object that will return the specified defaultValue if
     * this object's property is not found.
     * @param defaultValue
     * @return Newly constructed Property object
     */
    default Property<T> orElse(T defaultValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new Property object that will fetch the property backed by the provided
     * key.  The return value of the supplier will be cached until the configuration has changed
     * 
     * @param delegate
     * @return Newly constructed Property object
     */
    default Property<T> orElseGet(String key) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Create a new Property object that will map the current object's property value
     * to a new type.  The return value of the mapper will be cached until the 
     * configuration has changed.  
     * 
     * Note that no orElseGet() calls may be made on a mapped property
     * 
     * @param delegate
     * @return Newly constructed Property object
     */
    default <S> Property<S> map(Function<T, S> mapper) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @return Key or path to the property 
     */
    String getKey();
}
