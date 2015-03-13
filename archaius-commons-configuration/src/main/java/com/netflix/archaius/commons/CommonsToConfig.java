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
package com.netflix.archaius.commons;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.archaius.config.AbstractConfig;

/**
 * Adaptor to allow an Apache Commons Configuration AbstractConfig to be used
 * as an Archaius2 Config
 * 
 * @author elandau
 *
 */
public class CommonsToConfig extends AbstractConfig {

    private final AbstractConfiguration config;
    
    public CommonsToConfig(AbstractConfiguration config) {
        super("");
        this.config = config;
    }

    @Override
    public boolean containsProperty(String key) {
        return config.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public String getRawString(String key) {
        return config.getString(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }
}
