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

import java.util.Iterator;
import java.util.function.BiConsumer;

public class SystemConfig extends AbstractConfig {

    public static final SystemConfig INSTANCE = new SystemConfig();

    @Override
    public Object getRawProperty(String key) {
        return System.getProperty(key);
    }

    @Override
    public boolean containsKey(String key) {
        return System.getProperty(key) != null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator<String> getKeys() {
        return new Iterator<String>() {
            Iterator<Object> obj = System.getProperties().keySet().iterator();
            
            @Override
            public boolean hasNext() {
                return obj.hasNext();
            }

            @Override
            public String next() {
                return obj.next().toString();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        System.getProperties().forEach((k, v) -> consumer.accept(k.toString(), v));
    }
}
