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

import com.netflix.archaius.api.Config;
import com.netflix.archaius.test.ConfigInterfaceTest;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

import java.util.Iterator;
import java.util.Map;

public class TypesafeConfigTest extends ConfigInterfaceTest {
    @Test
    public void simple() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a=b"));
        Assert.assertEquals("b", config.getString("a"));
        Assert.assertTrue(config.containsKey("a"));
    }

    @Test
    public void simplePath() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a.b.c=foo"));
        Assert.assertEquals("foo", config.getString("a.b.c"));
        Assert.assertTrue(config.containsKey("a.b.c"));
    }

    @Test
    public void nested() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a { b { c=foo } }"));
        Assert.assertEquals("foo", config.getString("a.b.c"));
        Assert.assertTrue(config.containsKey("a.b.c"));
    }

    @Test
    public void keyWithAt() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("\"@a\"=b"));
        Assert.assertEquals("b", config.getString("@a"));
        Assert.assertTrue(config.containsKey("@a"));
    }

    @Test
    public void pathWithAt() throws ConfigException {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a.\"@b\".c=foo"));
        Assert.assertEquals("foo", config.getString("a.@b.c"));
        Assert.assertTrue(config.containsKey("a.@b.c"));
    }

    @Test
    public void specialChars() throws ConfigException {
        for (char c = '!'; c <= '~'; ++c) {
            if (c == '.') continue;
            String k = c + "a";
            String escaped = k.replace("\\", "\\\\").replace("\"", "\\\"");
            Config mc = MapConfig.builder().put(k, "b").build();
            Config tc = new TypesafeConfig(ConfigFactory.parseString("\"" + escaped + "\"=b"));
            Assert.assertEquals(mc.getString(k), tc.getString(k));
            Assert.assertEquals(mc.containsKey(k), tc.containsKey(k));
        }
    }

    @Test
    public void iterate() throws Exception {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a { \"@env\"=prod }"));
        Assert.assertEquals("prod", config.getString("a.@env"));

        // Make sure we can get all keys we get back from the iterator
        Iterator<String> ks = config.getKeys();
        while (ks.hasNext()) {
            String k = ks.next();
            config.getString(k);
        }
    }

    @Override
    protected Config getInstance(Map<String, String> properties) {
        return new TypesafeConfig(ConfigFactory.parseMap(properties));
    }
}
