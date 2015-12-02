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
 * A dynamic property that contains boolean value.
 * <p>Use APIs in {@link DynamicPropertyFactory} to create instance of this class.
 * 
 * @author awang
 *
 */
public class DynamicBooleanProperty extends PropertyWrapper<Boolean> {

    protected volatile boolean primitiveValue;

    public DynamicBooleanProperty(String propName, boolean defaultValue) {
        super(propName, Boolean.valueOf(defaultValue));

        // Set the initial value of the cached primitive value.
        this.primitiveValue = chooseValue();
    }

    @Override
    protected void propertyChanged() {
        super.propertyChanged();

        // Update the cached primitive value when the property is changed.
        this.primitiveValue = chooseValue();
    }

    /**
     * Get the current value from the underlying DynamicProperty
     *
     * @return
     */
    private boolean chooseValue() {
        return prop.getBoolean(defaultValue).booleanValue();
    }

    /**
     * Get the current cached value.
     *
     * @return
     */
    public boolean get() {
        return primitiveValue;
    }

    @Override
    public Boolean getValue() {
        return get();
    }
}
