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
 * An implementation of {@link DynamicDoubleProperty} that caches the primitive value whenever it is changed.
 *
 * This can give improved performance due to avoiding the unboxing, at the expense of additional
 * memory usage.
 *
 * @author Mike Smith
 *
 */
public class CachedDynamicDoubleProperty extends DynamicDoubleProperty {

    protected volatile double primitiveValue;

    public CachedDynamicDoubleProperty(String propName, double defaultValue) {
        super(propName, defaultValue);

        // Set the initial value of the cached primitive value.
        this.primitiveValue = chooseValue();
    }

    @Override
    protected void propertyChanged() {
        // Update the cached primitive value when the property is changed.
        this.primitiveValue = chooseValue();
    }

    /**
     * Get the current value from the underlying DynamicProperty
     *
     * @return
     */
    protected double chooseValue() {
        return prop.getDouble(defaultValue).doubleValue();
    }

    /**
     * Get the current cached value.
     *
     * @return
     */
    @Override
    public double get() {
        return primitiveValue;
    }

    @Override
    public Double getValue() {
        return get();
    }
}
