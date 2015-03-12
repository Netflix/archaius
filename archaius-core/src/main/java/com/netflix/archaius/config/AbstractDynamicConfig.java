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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import com.netflix.archaius.DynamicConfig;
import com.netflix.archaius.DynamicConfigObserver;

/**
 * Contract for a DynamicConfig source.
 * 
 * @author elandau
 */
public abstract class AbstractDynamicConfig extends AbstractConfig implements DynamicConfig {
    private CopyOnWriteArrayList<DynamicConfigObserver> listeners = new CopyOnWriteArrayList<DynamicConfigObserver>();
    
    public interface Listener {
        void onInvalidate(Collection<String> keys);
    }
    
    public AbstractDynamicConfig(String name) {
        super(name);
    }

    @Override
    public void addListener(DynamicConfigObserver listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(DynamicConfigObserver listener) {
        listeners.remove(listener);
    }
    
    protected void notifyOnUpdate(String key) {
        for (DynamicConfigObserver listener : listeners) {
            listener.onUpdate(key, this);
        }
    }
    
    protected void notifyOnUpdate() {
        for (DynamicConfigObserver listener : listeners) {
            listener.onUpdate(this);
        }
    }
    
    protected void notifyOnError(Throwable t) {
        for (DynamicConfigObserver listener : listeners) {
            listener.onError(t, this);
        }
    }
}
