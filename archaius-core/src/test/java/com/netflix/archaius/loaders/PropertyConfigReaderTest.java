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
package com.netflix.archaius.loaders;

import java.util.Iterator;

import org.junit.Test;

import com.netflix.archaius.Config;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertyConfigReaderTest {
    @Test
    public void readerTest() throws ConfigException{
        PropertiesConfigReader reader = new PropertiesConfigReader();
        Config config = reader.load(null, "apps", "application");
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            System.out.println("Key : " + key + " " + config.getString(key));
        }
        
        System.out.println(config.getList("application.list"));
        System.out.println(config.getList("application.list2"));
        System.out.println(config.getList("application.map"));
        System.out.println(config.getList("application.set"));
        
//        System.out.println(config.getBoolean("application.list"));
//        System.out.println(config.getInteger("application.list"));
    }
}
