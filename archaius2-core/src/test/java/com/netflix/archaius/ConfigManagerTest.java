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
package com.netflix.archaius;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.config.DefaultCompositeConfig;
import org.junit.Test;

import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.property.DefaultPropertyListener;

public class ConfigManagerTest {
    @Test
    public void testBasicReplacement() throws ConfigException {
        DefaultSettableConfig dyn = new DefaultSettableConfig();

        CompositeConfig config = new DefaultCompositeConfig();
        
        config.addConfig("dyn", dyn);
        config.addConfig("map", (MapConfig.builder()
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .put("c",      123)
                        .build()));
        
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<String> prop = factory.getProperty("abc").asString("defaultValue");
        
        prop.addListener(new DefaultPropertyListener<String>() {
            @Override
            public void onChange(String next) {
                System.out.println("Configuration changed : " + next);
            }
        });
        
        dyn.setProperty("abc", "${c}");
    }
}
