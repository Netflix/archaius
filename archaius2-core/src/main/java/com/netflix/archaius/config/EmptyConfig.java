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

public final class EmptyConfig extends AbstractConfig {

    public static final EmptyConfig INSTANCE = new EmptyConfig();
    
    private EmptyConfig() {
    }
    
    @Override
    public boolean containsKey(String key) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Iterator<String> getKeys() {
        return Collections.emptyIterator();
    }

    @Override
    public Object getRawProperty(String key) {
        return null;
    }

    @Override
    public void forEach(BiConsumer<String, Object> consumer) {
    }
}
