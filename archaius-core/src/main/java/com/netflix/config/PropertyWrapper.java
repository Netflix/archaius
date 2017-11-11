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

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.validation.PropertyChangeValidator;
import com.netflix.config.validation.ValidationException;

/**
 * A wrapper around DynamicProperty and associates it with a type.
 *
 * @param <V> The type of the DynamicProperty
 * @author awang
 */
public abstract class PropertyWrapper<V> implements Property<V> {
    protected final DynamicProperty prop;
    protected final V defaultValue;
    @SuppressWarnings("rawtypes")
    private static final IdentityHashMap<Class<? extends PropertyWrapper>, Object> SUBCLASSES_WITH_NO_CALLBACK
            = new IdentityHashMap<Class<? extends PropertyWrapper>, Object>();
    private static final Logger logger = LoggerFactory.getLogger(PropertyWrapper.class);
    private final Set<Runnable> callbacks = new CopyOnWriteArraySet<Runnable>();

    static {
        PropertyWrapper.registerSubClassWithNoCallback(DynamicIntProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicStringProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicBooleanProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicFloatProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicLongProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicDoubleProperty.class);
    }


    private static final Object DUMMY_VALUE = new Object();

    /**
     * By default, a subclass of PropertyWrapper will automatically register {@link #propertyChanged()} as a callback
     * for property value change. This method provide a way for a subclass to avoid this overhead if it is not interested
     * to get callback.
     *
     * @param c
     */
    public static final void registerSubClassWithNoCallback(@SuppressWarnings("rawtypes") Class<? extends PropertyWrapper> c) {
        SUBCLASSES_WITH_NO_CALLBACK.put(c, DUMMY_VALUE);
    }
    
    protected PropertyWrapper(String propName, V defaultValue) {
        this.prop = DynamicProperty.getInstance(propName);
        this.defaultValue = defaultValue;
        Class<?> c = getClass();
        // this checks whether this constructor is called by a class that
        // extends the immediate sub classes (IntProperty, etc.) of PropertyWrapper. 
        // If yes, it is very likely that propertyChanged()  is overriden
        // in the sub class and we need to register the callback.
        // Otherwise, we know that propertyChanged() does nothing in 
        // immediate subclasses and we can avoid registering the callback, which
        // has the cost of modifying the CopyOnWriteArraySet
        if (!SUBCLASSES_WITH_NO_CALLBACK.containsKey(c)) {
            Runnable callback = new Runnable() {
                public void run() {
                    propertyChanged();
                }
            };
            this.prop.addCallback(callback);
            callbacks.add(callback);
            this.prop.addValidator(new PropertyChangeValidator() {                
                @Override
                public void validate(String newValue) {
                    PropertyWrapper.this.validate(newValue);
                }
            });
            try {
                if (this.prop.getString() != null) {
                    this.validate(this.prop.getString());
                }
            } catch (ValidationException e) {
                logger.warn("Error validating property at initialization. Will fallback to default value.", e);
                prop.updateValue(defaultValue);
            }
        }
    }

    @Override
    public String getName() {
        return prop.getName();
    }

    public void addValidator(PropertyChangeValidator v) {
        if (v != null) {
            prop.addValidator(v);
        }
    }
    
    /**
     * Called when the property value is updated.
     * The default does nothing.
     * Subclasses are free to override this if desired.
     */
    protected void propertyChanged() {
        propertyChanged(getValue());
    }

    /**
     * Called when the property value is updated.
     * The default does nothing.
     * Subclasses are free to override this if desired.
     */
    protected void propertyChanged(V newValue) {
        // by default, do nothing
    }

    protected void validate(String newValue) {
    }
    
    /**
     * Gets the time (in milliseconds past the epoch) when the property
     * was last set/changed.
     */
    @Override
    public long getChangedTimestamp() {
        return prop.getChangedTimestamp();
    }

    /**
     * Add the callback to be triggered when the value of the property is changed
     *
     * @param callback
     */
    @Override
    public void addCallback(Runnable callback) {
        if (callback != null) {
            prop.addCallback(callback);
            callbacks.add(callback);
        }
    }

    /**
     * Remove all callbacks registered through this instance of property
     */
    @Override
    public void removeAllCallbacks() {
        final Set<Runnable> callbacksToRemove = new HashSet<Runnable>(callbacks);
        for (Runnable callback : callbacksToRemove) {
            prop.removeCallback(callback);
        }
        callbacks.removeAll(callbacksToRemove);
    }

    /**
     * Get current typed value of the property.
     */
    @Override
    public abstract V getValue();
    
    @Override
    public V getDefaultValue() {
        return defaultValue;
    }
    
    public DynamicProperty getDynamicProperty() {
        return prop;
    }

    @Override
    public String toString() {
        return "DynamicProperty: {name=" + prop.getName() + ", current value="
                + prop.getString(String.valueOf(defaultValue)) + "}";
    }
}
