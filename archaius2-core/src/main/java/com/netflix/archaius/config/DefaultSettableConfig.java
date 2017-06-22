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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.BiConsumer;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;

public class DefaultSettableConfig extends AbstractConfig implements SettableConfig {
    private volatile Map<String, Object> props = Collections.emptyMap();
    
    @Override
    public synchronized <T> void setProperty(String propName, T propValue) {
        Map<String, Object> copy = new HashMap<>(props.size() + 1);
        copy.putAll(props);
        copy.put(propName, propValue);
        props = copy;
        notifyConfigUpdated(this);
    }
    
    @Override
    public void clearProperty(String propName) {
        if (props.containsKey(propName)) {
            synchronized (this) {
                Map<String, Object> copy = new HashMap<>(props);
                copy.remove(propName);
                props = copy;
                notifyConfigUpdated(this);
            }
        }
    }

    @Override
    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return props.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        return props.get(key);
    }
    
    @Override
    public Iterator<String> getKeys() {
        return props.keySet().iterator();
    }

    @Override
    public void setProperties(Properties src) {
        if (null != src) {
            synchronized (this) {
                Map<String, Object> copy = new HashMap<>(props.size() + src.size());
                copy.putAll(props);
                for (Entry<Object, Object> prop : src.entrySet()) {
                    copy.put(prop.getKey().toString(), prop.getValue());
                }
                props = copy;
                notifyConfigUpdated(this);
            }
        }
    }

    @Override
    public void setProperties(Config src) {
        if (null != src) {
            synchronized (this) {
                Map<String, Object> copy = new HashMap<>(props);
                src.forEachProperty(copy::put);
                props = copy;
                notifyConfigUpdated(this);
            }
        }
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        props.forEach(consumer);
    }
}
