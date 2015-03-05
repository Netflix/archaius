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

/**
 * An abstract {@link PropertyListener} for use by different
 * components who need to listen for configuration changes. Users only need to
 * implement the {@link #handlePropertyEvent(String, Object, EventType)} method.
 * 
 * @author pkamath
 */
public abstract class AbstractDynamicPropertyListener implements  PropertyListener {

    public enum EventType {
        ADD, SET, CLEAR
    };
    
    
    @Override
    public void addProperty(Object source, String name, Object value,
            boolean beforeUpdate) {
        if (!beforeUpdate) {
            handlePropertyEvent(name, value, EventType.ADD);
        }
    }

    @Override
    public void clear(Object source, boolean beforeUpdate) {
        // no op - only interested in adds, sets and clears at a property leve!
    }

    @Override
    public void clearProperty(Object source, String name, Object value,
            boolean beforeUpdate) {
        if (!beforeUpdate) {
            handlePropertyEvent(name, value, EventType.CLEAR);
        }
    }

    @Override
    public void configSourceLoaded(Object source) {
        // no op - only interested in adds, sets and clears!
    }

    @Override
    public void setProperty(Object source, String name, Object value,
            boolean beforeUpdate) {
        if (!beforeUpdate) {
            handlePropertyEvent(name, value, EventType.SET);
        }
    }

    public abstract void handlePropertyEvent(String name, Object value,
            EventType eventType);
    
}
