/**
 * Copyright 2013 Netflix, Inc.
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
    public DynamicLongProperty(String propName, long defaultValue) {
        super(propName, Long.valueOf(defaultValue));
    }
        
    /**
     * Get the current value from the underlying DynamicProperty
     */
    public long get() {
        return prop.getLong(defaultValue).longValue();
    }

    @Override
    public Long getValue() {
        // TODO Auto-generated method stub
        return get();
    }
}
