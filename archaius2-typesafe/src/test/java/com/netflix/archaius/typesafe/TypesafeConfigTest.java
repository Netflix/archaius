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
package com.netflix.archaius.typesafe;

import com.netflix.archaius.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class TypesafeConfigTest {
    @Test
    public void simple() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a=b"));
        Assert.assertEquals("b", config.getString("a"));
    }

    @Test
    public void keyWithAt() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("\"@a\"=b"));
        Assert.assertEquals("b", config.getString("@a"));
    }

    @Test
    public void specialChars() throws ConfigException {
        for (char c = '!'; c <= '~'; ++c) {
            String k = c + "a";
            String escaped = k.replace("\\", "\\\\").replace("\"", "\\\"");
            Config mc = MapConfig.builder().put(k, "b").build();
            Config tc = new TypesafeConfig(ConfigFactory.parseString("\"" + escaped + "\"=b"));
            Assert.assertEquals(mc.getString(k), tc.getString(k));
            Assert.assertEquals(mc.containsKey(k), tc.containsKey(k));
        }
    }
}
