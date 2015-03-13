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
package com.netflix.config;

import java.util.Iterator;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

import com.netflix.archaius.config.AbstractDynamicConfig;

public class PropertyListenerToDynamicConfig extends AbstractDynamicConfig {

    private DynamicConfiguration config;

    public PropertyListenerToDynamicConfig(String name, DynamicConfiguration config) {
        super(name);
        this.config = config;
        this.config.addConfigurationListener(new ConfigurationListener() {
            @Override
            public void configurationChanged(ConfigurationEvent event) {
                if (!event.isBeforeUpdate()) {
                    switch (event.getType()) {
                    case AbstractConfiguration.EVENT_ADD_PROPERTY:
                    case AbstractConfiguration.EVENT_SET_PROPERTY:
                    case AbstractConfiguration.EVENT_CLEAR_PROPERTY:
                        notifyOnUpdate(event.getPropertyName());
                        break;
                    case AbstractConfiguration.EVENT_CLEAR:
                        notifyOnUpdate();
                        break;
                    }
                }
            }
        });
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
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    public String getRawString(String key) {
        // TODO: This could be a list
        return config.getProperty(key).toString();
    }
}
