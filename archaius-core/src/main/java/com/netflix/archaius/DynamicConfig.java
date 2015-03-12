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

/**
 * SPI for listening to DynamicConfig.  Note that this API does not and should not provide
 * any mechanism to set properties on the DynamicConfig.  Instead the concrete implementation
 * shall be responsible for refreshing the configuration.
 * 
 * @author elandau
 *
 */
public interface DynamicConfig extends Config {
    /**
     * Register a listener that will receive a call for each property that is added, removed
     * or updated.  It is recommended that the callbacks be invoked only after a full refresh
     * of the properties to ensure they are in a consistent state.
     * 
     * @param listener
     */
    void addListener(DynamicConfigObserver listener);

    /**
     * Remove a previously registered listener.
     * @param listener
     */
    void removeListener(DynamicConfigObserver listener);

}
