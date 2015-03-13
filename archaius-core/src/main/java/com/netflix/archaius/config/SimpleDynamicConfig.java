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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleDynamicConfig extends AbstractDynamicConfig implements SettableConfig {
    public SimpleDynamicConfig(String name) {
        super(name);
    }

    private ConcurrentMap<String, String> props = new ConcurrentHashMap<String, String>();
    
    @Override
    public <T> void setProperty(String propName, T propValue) {
        props.put(propName, propValue.toString());
        notifyOnUpdate(propName);
    }
    
    @Override
    public void clearProperty(String propName) {
        props.remove(propName);
        notifyOnUpdate(propName);
    }

    @Override
    public boolean containsProperty(String key) {
        return props.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return props.isEmpty();
    }

    @Override
    public String getRawString(String key) {
        return props.get(key);
    }
    
    @Override
    public Iterator<String> getKeys() {
        return props.keySet().iterator();
    }

    @Override
    public void setProperties(Properties properties) {
        if (null != properties) {
            for (Entry<Object, Object> prop : properties.entrySet()) {
                setProperty(prop.getKey().toString(), prop.getValue());
            }
        }
    }

}
