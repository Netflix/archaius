/*
 *
 *  Copyright 2013-2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.netflix.config;


/**
 * Some wrappers around DynamicPropertyS that cache the values and setup updates on callback. This is a performance optimization
 * to avoid the overhead on calling DynamicProperty.get() which was found under when profiling under high load.
 *
 * @author Mike Smith
 * Date: 11/20/15
 */
public class CachedProperties
{
    public static class Boolean
    {
        private DynamicBooleanProperty fastProperty;
        private volatile boolean value;

        public Boolean(java.lang.String propName, boolean initialValue)
        {
            fastProperty = DynamicPropertyFactory.getInstance().getBooleanProperty(propName, initialValue);
            value = fastProperty.get();

            // Add a callback to update the volatile value when the property is changed.
            fastProperty.addCallback(() -> value = fastProperty.get() );
        }

        public boolean get() {
            return value;
        }

        public void addCallback(Runnable callback) {
            fastProperty.addCallback(callback);
        }
    }

    public static class Int
    {
        private DynamicIntProperty fastProperty;
        private volatile int value;

        public Int(java.lang.String propName, int initialValue)
        {
            fastProperty = DynamicPropertyFactory.getInstance().getIntProperty(propName, initialValue);
            value = fastProperty.get();

            // Add a callback to update the volatile value when the property is changed.
            fastProperty.addCallback(() -> value = fastProperty.get() );
        }

        public int get() {
            return value;
        }

        public void addCallback(Runnable callback) {
            fastProperty.addCallback(callback);
        }
    }

    public static class Long
    {
        private DynamicLongProperty fastProperty;
        private volatile long value;

        public Long(java.lang.String propName, long initialValue)
        {
            fastProperty = DynamicPropertyFactory.getInstance().getLongProperty(propName, initialValue);
            value = fastProperty.get();

            // Add a callback to update the volatile value when the property is changed.
            fastProperty.addCallback(() -> value = fastProperty.get() );
        }

        public long get() {
            return value;
        }

        public void addCallback(Runnable callback) {
            fastProperty.addCallback(callback);
        }
    }

    public static class Double
    {
        private DynamicDoubleProperty fastProperty;
        private volatile double value;

        public Double(java.lang.String propName, double initialValue)
        {
            fastProperty = DynamicPropertyFactory.getInstance().getDoubleProperty(propName, initialValue);
            value = fastProperty.get();

            // Add a callback to update the volatile value when the property is changed.
            fastProperty.addCallback(() -> value = fastProperty.get() );
        }

        public double get() {
            return value;
        }

        public void addCallback(Runnable callback) {
            fastProperty.addCallback(callback);
        }
    }

    public static class String
    {
        private DynamicStringProperty fastProperty;
        private volatile java.lang.String value;

        public String(java.lang.String propName, java.lang.String initialValue)
        {
            fastProperty = DynamicPropertyFactory.getInstance().getStringProperty(propName, initialValue);
            value = fastProperty.get();

            // Add a callback to update the volatile value when the property is changed.
            fastProperty.addCallback(() -> value = fastProperty.get() );
        }

        public java.lang.String get() {
            return value;
        }

        public void addCallback(Runnable callback) {
            fastProperty.addCallback(callback);
        }
    }
}
