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
public class MapConfig<T> extends AbstractConfig {

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
    public static class Builder<T> {
        Map<String, T> map = new HashMap<>();
        
        public Builder put(String key, T value) {
            map.put(key, value);
            return this;
        }
        
        public MapConfig build() {
            return new MapConfig(map);
        }
    }
    
    public static <T> Builder builder() {
        return new Builder<T>();
    }
    
    public static MapConfig<String> from(Properties props) {
        Map<String, String> tempMap = new HashMap<>(props.size());

        for (Entry<Object, Object> entry : props.entrySet()) {
            tempMap.put(entry.getKey().toString(), entry.getValue().toString());
        }

        return new MapConfig<String>(Collections.unmodifiableMap(tempMap));
    }
    
    public static <T> MapConfig from(Map<String, T> props) {
        return new MapConfig<T>(props);
    }
    
    private Map<String, T> props = new HashMap<>();
    
    /**
     * Construct a MapConfig as a copy of the provided Map
     * @param props
     */
    public MapConfig(Map<String, T> props) {
        this.props.putAll(props);
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
