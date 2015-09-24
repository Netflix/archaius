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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import org.junit.Test;

import com.netflix.archaius.Config;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.readers.PropertiesConfigReader;

public class PropertyConfigReaderTest {
    @Test
    public void readerTest() throws ConfigException{
        PropertiesConfigReader reader = new PropertiesConfigReader();
        Config config = reader.load(null, "application", CommonsStrInterpolator.INSTANCE, new StrInterpolator.Lookup() {
            @Override
            public String lookup(String key) {
                return null;
            }
        });
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            System.out.println("Key : " + key + " " + config.getString(key));
        }
        
        assertThat(Arrays.asList("b"), is(config.getList("application.list", String.class)));
        assertThat(Arrays.asList("a", "b"), equalTo(config.getList("application.list2", String.class)));
//        assertThat(Arrays.asList("b"), config.getList("application.map"));
        assertThat(Arrays.asList("a", "b"), is(config.getList("application.set", String.class)));
        assertThat("a,b,c", is(config.getString("application.valuelist")));
        assertThat(Arrays.asList("a","b","c"), is(config.getList("application.valuelist", String.class)));
        
//        System.out.println(config.getBoolean("application.list"));
//        System.out.println(config.getInteger("application.list"));
    }
}
