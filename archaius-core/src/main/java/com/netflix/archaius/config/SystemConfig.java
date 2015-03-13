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
import java.util.Properties;

public class SystemConfig extends AbstractConfig {

    private static final String DEFAULT_NAME = "SYSTEM";
    
    private final Properties props;
    
    public SystemConfig() {
        this(DEFAULT_NAME);
    }

    public SystemConfig(String name) {
        super(name);
        props = System.getProperties();
    }

    @Override
    public String getRawString(String key) {
        return props.getProperty(key);
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
    public Iterator<String> getKeys() {
        return new Iterator<String>() {
            Iterator<Object> obj = props.keySet().iterator();
            
            @Override
            public boolean hasNext() {
                return obj.hasNext();
            }

            @Override
            public String next() {
                return obj.next().toString();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
