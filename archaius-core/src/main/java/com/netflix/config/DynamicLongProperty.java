/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config;

/**
 * A dynamic property whose value is a long.
 * <p>Use APIs in {@link DynamicPropertyFactory} to create instance of this class.
 * 
 * @author awang
 *
 */
public class DynamicLongProperty extends PropertyWrapper<Long> {

    protected volatile long primitiveValue;

    public DynamicLongProperty(String propName, long defaultValue) {
        super(propName, Long.valueOf(defaultValue));

        // Set the initial value of the cached primitive value.
        this.primitiveValue = chooseValue();

        // Add a callback to update the cached primitive value when the property is changed.
        this.prop.addCallback(new Runnable() {
            @Override
            public void run() {
                primitiveValue = chooseValue();
            }
        });
    }

    /**
     * Get the current value from the underlying DynamicProperty
     *
     * @return
     */
    private long chooseValue() {
        Long propValue = this.prop == null ? null : this.prop.getLong(defaultValue);
        return propValue == null ? defaultValue : propValue.longValue();
    }

    /**
     * Get the current cached value.
     *
     * @return
     */
    public long get() {
        return primitiveValue;
    }

    @Override
    public Long getValue() {
        return get();
    }
}
