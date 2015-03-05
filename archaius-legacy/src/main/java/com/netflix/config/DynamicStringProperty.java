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
 * A dynamic property whose value is a string.
 * <p>Use APIs in {@link DynamicPropertyFactory} to create instance of this class.
 * 
 * @author awang
 *
 */
public class DynamicStringProperty extends PropertyWrapper<String> {

    public DynamicStringProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }

    /**
     * Get the current value from the underlying DynamicProperty
     */
    public String get() {
        return prop.getString(defaultValue);
    }

    @Override
    public String getValue() {
        return get();
    }
}
