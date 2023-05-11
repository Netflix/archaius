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

/**
 * Handler for property change notifications for a single property key
 * 
 * @param <T>
 */
public interface PropertyListener<T> extends Consumer<T> {
    /**
     * Notification that the property value changed.  next=null indicates that the property
     * has been deleted.
     * 
     * @param value The new value for the property.
     */
    @Deprecated
    void onChange(T value);

    @Override
    default void accept(T value) {
        onChange(value);
    }
    
    /**
     * Notification that a property update failed
     * @param error
     * @deprecated This method isn't actually used by anyone.  Parse errors will be handled in Config
     */
    @Deprecated
    default void onParseError(Throwable error) {};
}
