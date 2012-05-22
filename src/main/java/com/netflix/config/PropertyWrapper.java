/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config;

import java.util.IdentityHashMap;

public abstract class PropertyWrapper<V> {
    protected DynamicProperty prop;
    protected V defaultValue;
    private static final IdentityHashMap<Class<? extends PropertyWrapper<?>>, Object> SUBCLASSES_WITH_NO_CALLBACK 
        = new IdentityHashMap<Class<? extends PropertyWrapper<?>>, Object>();

    static {
        PropertyWrapper.registerSubClassWithNoCallback(DynamicIntProperty.class);    
        PropertyWrapper.registerSubClassWithNoCallback(DynamicStringProperty.class);    
        PropertyWrapper.registerSubClassWithNoCallback(DynamicBooleanProperty.class);    
        PropertyWrapper.registerSubClassWithNoCallback(DynamicFloatProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicLongProperty.class);
        PropertyWrapper.registerSubClassWithNoCallback(DynamicDoubleProperty.class);
    }


    private static final Object DUMMY_VALUE = new Object(); 
    
    public static final void registerSubClassWithNoCallback(Class<? extends PropertyWrapper<?>> c) {
        SUBCLASSES_WITH_NO_CALLBACK.put(c, DUMMY_VALUE);
    }
    
    protected PropertyWrapper(String propName, V defaultValue) {
        this.prop = DynamicProperty.getInstance(propName);
        this.defaultValue = defaultValue;
        Class c = getClass();
        // this checks whether this constructor is called by a class that
        // extends the immediate sub classes (IntProperty, etc.) of PropertyWrapper. 
        // If yes, it is very likely that propertyChanged()  is overriden
        // in the sub class and we need to register the callback.
        // Otherwise, we know that propertyChanged() does nothing in 
        // immediate subclasses and we can avoid registering the callback, which
        // has the cost of modifying the CopyOnWriteArraySet
        if (!SUBCLASSES_WITH_NO_CALLBACK.containsKey(c)) {
            this.prop.addCallback(new Runnable() {
                                      public void run() {
                                          propertyChanged();
                                      }
                                  });
        }
    }
    
    public String getName() {
        return prop.getName();
    }
    /**
     * Called when the property value is updated.
     * The default does nothing.
     * Subclasses are free to override this if desired.
     */
    protected void propertyChanged() {
        // by default, do nothing
    }
    /**
     * Gets the time (in milliseconds past the epoch) when the property
     * was last set/changed.
     */
    public long getChangedTimestamp() {
        return prop.getChangedTimestamp();
    }
    
    public void addCallback(Runnable callback) {
        prop.addCallback(callback);
    }
    
    public abstract V getValue();
    
    @Override
    public String toString() {
        return "DynamicProperty: {name=" + prop.getName() + ", current value="
                + prop.getString(defaultValue.toString()) + "}";
    }
}
