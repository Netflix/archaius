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

import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Test;

public class AbstractConfigTest {

    private final AbstractConfig config = new AbstractConfig() {
        @Override
        public boolean containsKey(String key) {
            return "foo".equals(key);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<String> getKeys() {
            return Collections.singletonList("foo").iterator();
        }

        @Override
        public Object getRawProperty(String key) {
            if ("foo".equals(key)) {
                return "bar";
            }
            return null;
        }

        @Override
        public void forEachProperty(BiConsumer<String, Object> consumer) {
            consumer.accept("foo",  "bar");
        }
    };

    @Test
    public void testGet() throws Exception {
        Assert.assertEquals("bar", config.get(String.class, "foo"));
    }
    
    @Test
    public void getExistingProperty() {
        Assert.assertEquals("bar", config.getProperty("foo").get());
    }
    
    @Test
    public void getNonExistentProperty() {
        Assert.assertFalse(config.getProperty("non_existent").isPresent());
    }
}
