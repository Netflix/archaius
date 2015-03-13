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
import java.util.Map;

public class EnvironmentConfig extends AbstractConfig {

    private static final String DEFAULT_NAME = "ENVIRONMENT";
    
    private final Map<String, String> properties;
    
    public EnvironmentConfig() {
        this(DEFAULT_NAME);
    }

    public EnvironmentConfig(String name) {
        super(name);
        this.properties = System.getenv();
    }

    @Override
    public String getRawString(String key) {
        return properties.get(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }
    
    @Override
    public Iterator<String> getKeys() {
        return properties.keySet().iterator();
    }
}
