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
import com.typesafe.config.ConfigFactory;

import com.netflix.archaius.config.MapConfig;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypesafeConfigTest {
    @Test
    public void simple() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a=b"));
        assertEquals("b", config.getString("a"));
        assertTrue(config.containsKey("a"));
        assertFalse(config.containsKey("foo"));
        assertEquals("bar", config.getString("foo", "bar"));
    }

    @Test
    public void simplePath() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a.b.c=foo"));
        assertEquals("foo", config.getString("a.b.c"));
        assertTrue(config.containsKey("a.b.c"));
    }

    @Test
    public void nested() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a { b { c=foo } }"));
        assertEquals("foo", config.getString("a.b.c"));
        assertTrue(config.containsKey("a.b.c"));
    }

    @Test
    public void keyWithAt() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("\"@a\"=b"));
        assertEquals("b", config.getString("@a"));
        assertTrue(config.containsKey("@a"));
    }

    @Test
    public void pathWithAt() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a.\"@b\".c=foo"));
        assertEquals("foo", config.getString("a.@b.c"));
        assertTrue(config.containsKey("a.@b.c"));
    }

    @Test
    public void specialChars() {
        for (char c = '!'; c <= '~'; ++c) {
            if (c == '.') continue;
            String k = c + "a";
            String escaped = k.replace("\\", "\\\\").replace("\"", "\\\"");
            Config mc = MapConfig.builder().put(k, "b").build();
            Config tc = new TypesafeConfig(ConfigFactory.parseString("\"" + escaped + "\"=b"));
            assertEquals(mc.getString(k), tc.getString(k));
            assertEquals(mc.containsKey(k), tc.containsKey(k));
        }
    }

    @Test
    public void iterate() {
        Config config = new TypesafeConfig(ConfigFactory.parseString("a { \"@env\"=prod }"));
        assertEquals("prod", config.getString("a.@env"));

        // Make sure we can get all keys we get back from the iterator
        @SuppressWarnings("deprecation")
        Iterator<String> ks = config.getKeys();
        while (ks.hasNext()) {
            String k = ks.next();
            config.getString(k);
        }

        Iterable<String> keysIterable = config.keys();
        for (String key : keysIterable) {
            config.getString(key);
        }
    }
}
