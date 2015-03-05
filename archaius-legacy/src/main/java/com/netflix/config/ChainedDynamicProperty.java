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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class that provides the chaining functionality of dynamic properties.
 * The idea is that the head property holds the current up-to-date value of the property and the "next" property 
 * in the chain serves as the default if the head property value does not exist or is not acceptable.
 * Concrete implementation is available for IntProperty, StringProperty, BooleanProperty and FloatProperty.
 * 
 * <p>For example
 * 
 * <pre>
 *  {@code
        DynamicStringProperty pString = DynamicPropertyFactory.getInstance().getStringProperty("defaultString", "default-default");
        ChainedDynamicProperty.StringProperty fString = new ChainedDynamicProperty.StringProperty("overrideString", pString);

        assertTrue("default-default".equals(fString.get()));

        ConfigurationManager.getConfigInstance().setProperty("defaultString", "default");
        assertTrue("default".equals(fString.get()));

        ConfigurationManager.getConfigInstance().setProperty("overrideString", "override");
        assertTrue("override".equals(fString.get()));

        ConfigurationManager.getConfigInstance().clearProperty("overrideString");
        assertTrue("default".equals(fString.get()));

        ConfigurationManager.getConfigInstance().clearProperty("defaultString");
        assertTrue("default-default".equals(fString.get()));

 *  }
 * </pre>
 *
 * @see IntProperty
 * @see StringProperty
 * @see BooleanProperty
 * @see FloatProperty
 */
public class ChainedDynamicProperty {

    private static final Logger logger = LoggerFactory.getLogger(ChainedDynamicProperty.class);

    public static abstract class ChainLink<T> implements Property<T> {

        private final AtomicReference<ChainLink<T>> pReference;
        private final ChainLink<T> next; 
        private final List<Runnable> callbacks; 
        private final T defaultValue;


        /**
         * @return Boolean
         */
        public abstract boolean isValueAcceptable();
        
        /**
         * Get the property referenced by this ChainLink
         * @return the referenced property
         */
        protected abstract Property<T> getReferencedProperty();

        /**
         * Used for end node
         */
        public ChainLink(T defaultValue) {
            next = null; 
            pReference = new AtomicReference<ChainLink<T>>(this);
            callbacks = new ArrayList<Runnable>();
            this.defaultValue = defaultValue;
        }

        /**
         * @param nextProperty
         */
        public ChainLink(ChainLink<T> nextProperty) {
            next = nextProperty; 
            pReference = new AtomicReference<ChainLink<T>>(next);
            callbacks = new ArrayList<Runnable>();
            this.defaultValue = nextProperty.getDefaultValue();
        }

        protected void checkAndFlip() {
            // in case this is the end node
            if(next == null) {
                pReference.set(this);
                return;
            }

            if (this.isValueAcceptable()) {
                logger.info("Flipping property: " + getName() + " to use it's current value:" + getValue());
                pReference.set(this);
            } else {
                logger.info("Flipping property: " + getName() + " to use NEXT property: " + next);
                pReference.set(next);
            }

            for (Runnable r : callbacks) {
                r.run();
            }
        }

        /**
         * @return T
         */
        public T get() {
            if (pReference.get() == this) {
                return this.getValue();
            } else {
                return pReference.get().get();
            }
        }
        
        @Override
        public T getValue() {
            return getReferencedProperty().getValue();
        }

        @Override
        public T getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String getName() {
            return getReferencedProperty().getName();
        }

        @Override
        public long getChangedTimestamp() {
            if (pReference.get() == this) {
                return getReferencedProperty().getChangedTimestamp();
            } else {
                return pReference.get().getChangedTimestamp();
            }
        }

        /**
         * @param r
         */
        @Override
        public void addCallback(Runnable r) {
            callbacks.add(r);
        }

        /**
         * Remove callback from callbacks list
         */
        @Override
        public void removeAllCallbacks() {
            callbacks.clear();
        }

        /**
         * @return String 
         */
        @Override
        public String toString() {
            return getName() + " = " + get();   
        }
    }

    public static class StringProperty extends ChainLink<String> {

        private final DynamicStringProperty sProp;
        private Runnable callback;

        public StringProperty(DynamicStringProperty sProperty) {
            super(sProperty.getDefaultValue());
            sProp = sProperty;
        }

        public StringProperty(String name, DynamicStringProperty sProperty) {
            this(name, new StringProperty(sProperty));
        }

        public StringProperty(String name, StringProperty next) {
            super(next); // setup next pointer

            sProp = DynamicPropertyFactory.getInstance().getStringProperty(name, null);
            callback = new Runnable() {
                @Override
                public void run() {
                    logger.info("Property changed: '" + getName() + " = " + getValue() + "'");
                    checkAndFlip();
                }
            };
            sProp.addCallback(callback);
            checkAndFlip();
        }

        @Override
        public boolean isValueAcceptable() {
            return (sProp.get() != null);
        }

        @Override
        protected Property<String> getReferencedProperty() {
            return sProp;
        }

        @Override
        public void removeAllCallbacks() {
            super.removeAllCallbacks();
            sProp.prop.removeCallback(callback);
        }
    }

    public static class IntProperty extends ChainLink<Integer> {

        private final DynamicIntProperty sProp;
        private Runnable callback;

        public IntProperty(DynamicIntProperty sProperty) {
            super(sProperty.getDefaultValue());
            sProp = sProperty;
        }

        public IntProperty(String name, DynamicIntProperty sProperty) {
            this(name, new IntProperty(sProperty));
        }

        public IntProperty(String name, IntProperty next) {
            super(next); // setup next pointer

            sProp = DynamicPropertyFactory.getInstance().getIntProperty(name, Integer.MIN_VALUE);
            callback = new Runnable() {
                @Override
                public void run() {
                    logger.info("Property changed: '" + getName() + " = " + getValue() + "'");
                    checkAndFlip();
                }
            };
            sProp.addCallback(callback);
            checkAndFlip();
        }

        @Override
        public boolean isValueAcceptable() {
            return (sProp.get() != Integer.MIN_VALUE);
        }

        @Override
        protected Property<Integer> getReferencedProperty() {
            return sProp;
        }

        @Override
        public void removeAllCallbacks() {
            super.removeAllCallbacks();
            sProp.prop.removeCallback(callback);
        }
    }

    public static class LongProperty extends ChainLink<Long> {

        private final DynamicLongProperty sProp;
        private Runnable callback;

        public LongProperty(DynamicLongProperty sProperty) {
            super(sProperty.getDefaultValue());
            sProp = sProperty;
        }

        public LongProperty(String name, DynamicLongProperty sProperty) {
            this(name, new LongProperty(sProperty));
        }

        public LongProperty(String name, LongProperty next) {
            super(next); // setup next pointer

            sProp = DynamicPropertyFactory.getInstance().getLongProperty(name, Long.MIN_VALUE);
            callback = new Runnable() {
                @Override
                public void run() {
                    logger.info("Property changed: '" + getName() + " = " + getValue() + "'");
                    checkAndFlip();
                }
            };
            sProp.addCallback(callback);
            checkAndFlip();
        }

        @Override
        public boolean isValueAcceptable() {
            return (sProp.get() != Long.MIN_VALUE);
        }

        @Override
        protected Property<Long> getReferencedProperty() {
            return sProp;
        }

        @Override
        public void removeAllCallbacks() {
            super.removeAllCallbacks();
            sProp.prop.removeCallback(callback);
        }
    }

    public static class FloatProperty extends ChainLink<Float> {

        private final DynamicFloatProperty sProp;
        private Runnable callback;

        public FloatProperty(DynamicFloatProperty sProperty) {
            super(sProperty.getDefaultValue());
            sProp = sProperty;
        }

        public FloatProperty(String name, DynamicFloatProperty sProperty) {
            this(name, new FloatProperty(sProperty));
        }

        public FloatProperty(String name, FloatProperty next) {
            super(next); // setup next pointer
            sProp = DynamicPropertyFactory.getInstance().getFloatProperty(name, Float.MIN_VALUE);
            callback = new Runnable() {
                @Override
                public void run() {
                    logger.info("Property changed: '" + getName() + " = " + getValue() + "'");
                    checkAndFlip();
                }
            };
            sProp.addCallback(callback);
            checkAndFlip();
        }

        @Override
        public boolean isValueAcceptable() {
            return Math.abs(sProp.get() - Float.MIN_VALUE) > 0.000001f ;
        }

        @Override
        protected Property<Float> getReferencedProperty() {
            return sProp;
        }

        @Override
        public void removeAllCallbacks() {
            super.removeAllCallbacks();
            sProp.prop.removeCallback(callback);
        }
    }

    public static class BooleanProperty extends ChainLink<Boolean> {

        private final DynamicBooleanPropertyThatSupportsNull sProp;
        private Runnable callback;

        public BooleanProperty(DynamicBooleanPropertyThatSupportsNull sProperty) {
            super(sProperty.getDefaultValue());
            sProp = sProperty;
        }

        public BooleanProperty(String name, DynamicBooleanPropertyThatSupportsNull sProperty) {
            this(name, new BooleanProperty(sProperty));
        }

        public BooleanProperty(String name, BooleanProperty next) {
            super(next); // setup next pointer

            sProp = new DynamicBooleanPropertyThatSupportsNull(name, null);
            callback = new Runnable() {
                @Override
                public void run() {
                    logger.info("Property changed: '" + getName() + " = " + getValue() + "'");
                    checkAndFlip();
                }
            };
            sProp.addCallback(callback);
            checkAndFlip();
        }

        @Override
        public boolean isValueAcceptable() {
            return (sProp.getValue() != null);
        }

        @Override
        protected Property<Boolean> getReferencedProperty() {
            return sProp;
        }

        @Override
        public void removeAllCallbacks() {
            super.removeAllCallbacks();
            sProp.prop.removeCallback(callback);
        }
    }

    public static class DynamicBooleanPropertyThatSupportsNull extends PropertyWrapper<Boolean> {
        public DynamicBooleanPropertyThatSupportsNull(String propName, Boolean defaultValue) {
            super(propName, defaultValue);
        }

        /**
         * Get the current value from the underlying DynamicProperty
         */
        public Boolean get() {
            return prop.getBoolean(defaultValue);
        }

        @Override
        public Boolean getValue() {
            return get();
        }
    }
}
