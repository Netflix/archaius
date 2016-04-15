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

/**
 * Config backed by an immutable map.
 */
public class MapConfig extends AbstractConfig {

    /**
     * The builder only provides convenience for fluent style adding of properties
     * 
     * {@code
     * <pre>
     * MapConfig.builder()
     *      .put("foo", "bar")
     *      .put("baz", 123)
     *      .build()
     * </pre>
     * }
     * @author elandau
     */
    public static class Builder {
        Map<String, Object> map = new HashMap<String, Object>();

        public <T> Builder put(String key, T value) {
            map.put(key, value);
            return this;
        }
        
        public MapConfig build() {
            return new MapConfig(map);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static MapConfig from(Properties props) {
        return new MapConfig(props);
    }

    public static MapConfig from(Map<String, Object> props) {
        return new MapConfig(props);
    }

    private Map<String, Object> props = new HashMap<String, Object>();

    /**
     * Construct a MapConfig as a copy of the provided Map
     * @param props
     */
    public MapConfig(Map<String, Object> props) {
        this.props.putAll(props);
        this.props = Collections.unmodifiableMap(this.props);
    }

    /**
     * Construct a MapConfig as a copy of the provided properties
     * @param name
     * @param props
     */
    public MapConfig(Properties props) {
        for (Entry<Object, Object> entry : props.entrySet()) {
            this.props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        this.props = Collections.unmodifiableMap(this.props);
    }
    
    @Override
    public Object getRawProperty(String key) {
        return props.get(key);
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
    public Iterator<String> getKeys() {
        return props.keySet().iterator();
    }

}
