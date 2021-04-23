/**
 * Copyright 2018 Netflix, Inc.
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

/**
 * A static {@link Property} whose value never changes. This is useful when creating
 * properties for use in tests.
 */
public class StaticProperty<T> implements Property<T> {

    private final String key;
    private final T value;

    public StaticProperty(String key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String getKey() {
        return key;
    }
}
