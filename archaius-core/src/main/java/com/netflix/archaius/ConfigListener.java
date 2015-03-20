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
 * Listener for property updates.  Due to the cascading nature of property value resolution
 * there's not much value in specifying the value or differentiating between set, add and
 * delete.  Instead the listener is expected to fetch the property value from the config.
 * 
 * @author elandau
 */
public interface ConfigListener {
    /**
     * Notification that a configuration was added.  This will normally only be called
     * for CompositeConfig derived implementations.
     * @param config
     */
    public void onConfigAdded(Config config);
    
    /**
     * Notification that a configuration was removed.  This will normally only be called
     * for CompositeConfig derived implementations.
     * @param config
     */
    public void onConfigRemoved(Config config);
    
    /**
     * Notify the listener that the value of a property has changed.  This is normally in 
     * response to an incremental update to a dynamic configuration.
     * 
     * @param propName
     */
    public void onConfigUpdated(String propName, Config config);

    /**
     * Notify the listener that the entire configuration of a child has changed.  This is 
     * normally in response to a snapshot update to a dynamic configuration. A listener will
     * likely respond to this by invalidating the entire property registration cache as it 
     * is more efficient than trying to determine the delta.
     * @param config
     */
    public void onConfigUpdated(Config config);
    
    /**
     * Notify of an error in the configuration listener.  The error indicates that the
     * DyanmicConfig was not able to update its configuration.  The DynamicConfig
     * should maintain the last known good state
     * 
     * @param error
     */
    public void onError(Throwable error, Config config);
}
