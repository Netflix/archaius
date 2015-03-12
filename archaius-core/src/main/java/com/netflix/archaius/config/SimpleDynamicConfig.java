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

import com.netflix.archaius.Config;

public class SimpleDynamicConfig extends AbstractDynamicConfig implements SettableConfig {
    public SimpleDynamicConfig(String name) {
        super(name);
    }

    private ConcurrentMap<String, Object> props = new ConcurrentHashMap<String, Object>();
    
    @Override
    public void setProperty(String propName, Object propValue) {
        props.put(propName, propValue);
        notifyOnUpdate(propName);
    }
    
    @Override
    public void clearProperty(String propName) {
        props.remove(propName);
        notifyOnUpdate(propName);
    }

    public void appendConfig(Config config) {
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            props.put(key, config.getRawProperty(key));
        }
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
    public Object getRawProperty(String key) {
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
