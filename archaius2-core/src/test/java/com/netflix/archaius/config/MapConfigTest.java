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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.exceptions.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapConfigTest {
    private final MapConfig config = MapConfig.builder()
            .put("str", "value")
            .put("badnumber", "badnumber")
            .build();
    
    @Test
    public void nonExistentString() {
        assertThrows(NoSuchElementException.class, () -> config.getString("nonexistent"));
    }
    
    @Test
    public void nonExistentBigDecimal() {
        assertThrows(NoSuchElementException.class, () -> config.getBigDecimal("nonexistent"));
    }
    
    @Test
    public void nonExistentBigInteger() {
        assertThrows(NoSuchElementException.class, () -> config.getBigInteger("nonexistent"));
    }
    
    @Test
    public void nonExistentBoolean() {
        assertThrows(NoSuchElementException.class, () -> config.getBoolean("nonexistent"));
    }
    
    @Test
    public void nonExistentByte() {
        assertThrows(NoSuchElementException.class, () -> config.getByte("nonexistent"));
    }
    
    @Test
    public void nonExistentDouble() {
        assertThrows(NoSuchElementException.class, () -> config.getDouble("nonexistent"));
    }
    
    @Test
    public void nonExistentFloat() {
        assertThrows(NoSuchElementException.class, () -> config.getFloat("nonexistent"));
    }
    
    @Test
    public void nonExistentInteger() {
        assertThrows(NoSuchElementException.class, () -> config.getInteger("nonexistent"));
    }
    
    @Test
    public void nonExistentList() {
        assertThrows(NoSuchElementException.class, () -> config.getList("nonexistent"));
    }
    
    @Test
    public void nonExistentLong() {
        assertThrows(NoSuchElementException.class, () -> config.getLong("nonexistent"));
    }
    
    @Test
    public void nonExistentShort() {
        assertThrows(NoSuchElementException.class, () -> config.getShort("nonexistent"));
    }
    
    @Test
    public void invalidBigDecimal() {
        assertThrows(ParseException.class, () -> config.getBigDecimal("badnumber"));
    }
    
    @Test
    public void invalidBigInteger() {
        assertThrows(ParseException.class, () -> config.getBigInteger("badnumber"));
    }
    
    @Test
    public void invalidBoolean() {
        assertThrows(ParseException.class, () -> config.getBoolean("badnumber"));
    }
    
    @Test
    public void invalidByte() {
        assertThrows(ParseException.class, () -> config.getByte("badnumber"));
    }
    
    @Test
    public void invalidDouble() {
        assertThrows(ParseException.class, () -> config.getDouble("badnumber"));
    }
    
    @Test
    public void invalidFloat() {
        assertThrows(ParseException.class, () -> config.getFloat("badnumber"));
    }
    
    @Test
    public void invalidInteger() {
        assertThrows(ParseException.class, () -> config.getInteger("badnumber"));
    }
    
    @Test
    public void invalidList() {
        assertThrows(ParseException.class, () -> config.getList("badnumber", Integer.class));
    }
    
    @Test
    public void invalidLong() {
        assertThrows(ParseException.class, () -> config.getLong("badnumber"));
    }
    
    @Test
    public void invalidShort() {
        assertThrows(ParseException.class, () -> config.getShort("badnumber"));
    }
    
    @Test
    public void interpolationShouldWork() {
        Config config = MapConfig.builder()
                .put("env",         "prod")
                .put("replacement", "${env}")
                .build();
        
        assertEquals("prod", config.getString("replacement"));
    }
    
    @Test
    public void interpolationWithDefaultReplacement() {
        Config config = MapConfig.builder()
                .put("env",         "prod")
                .put("replacement", "${env}")
                .build();
        
       assertEquals("prod", config.getString("nonexistent", "${env}"));
    }
    
    @Test
    public void infiniteInterpolationRecursionShouldFail() {
        Config config = MapConfig.builder()
                .put("env", "${env}")
                .put("replacement.env", "${env}")
                .build();

        assertThrows(IllegalStateException.class, () -> config.getString("replacement.env"));
    }
    
    @Test
    public void numericInterpolationShouldWork() {
        Config config = MapConfig.builder()
                .put("default",     "123")
                .put("value",       "${default}")
                .build();
        assertEquals(123L, (long) config.getLong("value"));
    }

    @Test
    public void getKeys() {
        Map<String, String> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", "value2");

        Config config = MapConfig.from(props);

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        assertTrue(keys.hasNext());

        Set<String> keySet = new HashSet<>();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }

        assertEquals(2, keySet.size());
        assertEquals(props.keySet(), keySet);
    }

    @Test
    public void getKeysIteratorRemoveThrows() {
        Config config = MapConfig.builder()
                .put("key1", "value1")
                .put("key2", "value2")
                .build();
        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();

        assertTrue(keys.hasNext());
        keys.next();
        assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() {
        Config config = MapConfig.builder()
                .put("key1", "value1")
                .put("key2", "value2")
                .build();
        Iterable<String> keys = config.keys();

        assertEquals(2, Iterables.size(keys));
        assertEquals(Sets.newHashSet("key1", "key2"), Sets.newHashSet(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        Config config = MapConfig.builder()
                .put("key1", "value1")
                .put("key2", "value2")
                .build();

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }
}
