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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.netflix.config.validation.PropertyChangeValidator;
import com.netflix.config.validation.ValidationException;

/**
 * A cached configuration property value that is automatically
 * updated when the config is changed.
 * The object is fully thread safe, and access is very fast.
 * (In fact, testing indicates that using a DynamicProperty is faster
 * than fetching a System Property.)
 * <p>
 * This class is intended for those situations where the value of
 * a property is fetched many times, and the value may be
 * changed on-the-fly.
 * If the property is being read only once, "normal" access methods
 * should be used.
 * If the property value is fixed, consider just caching the value
 * in a variable.
 * <p>
 * Fetching the cached value is synchronized only on this property,
 * so contention should be negligible.
 * If even that level of overhead is too much for you,
 * you should (a) think real hard about what you are doing, and
 * (b) just cache the property value in a variable and be done
 * with it.
 * <p>
 * <b>IMPORTANT NOTE</b>
 * <br>
 * DynamicProperty objects are not subject to normal garbage collection.
 * They should be used only as a static value that lives for the
 * lifetime of the program.
 *
 * @author slanning 
 */
public class DynamicProperty {

    private static final Logger logger = LoggerFactory.getLogger(DynamicProperty.class);
    private volatile static DynamicPropertySupport dynamicPropertySupportImpl;

    /*
     * Cache update is handled by a single configuration listener,
     * with a static collection holding all defined DynamicProperty objects.
     * It is assumed that DynamicProperty objects are static and never
     * subject to gc, so holding them in the collection does not cause
     * a memory leak.
     */
    
    private static final ConcurrentHashMap<String, DynamicProperty> ALL_PROPS
        = new ConcurrentHashMap<String, DynamicProperty>();
    
    private Object lock = new Object();         // synchs caches and updates
    private String propName;
    private String stringValue = null;
    private long changedTime;
    private CopyOnWriteArraySet<Runnable> callbacks = new CopyOnWriteArraySet<Runnable>();
    private CopyOnWriteArraySet<PropertyChangeValidator> validators = new CopyOnWriteArraySet<PropertyChangeValidator>();


    /**
     * A cached value of a particular type.
     * @param <T> the type of the cached value
     */
    private abstract class CachedValue<T> {
        private volatile boolean isCached;
        private volatile IllegalArgumentException exception;
        private volatile T value;
        public CachedValue() {
            flush();
        }
        /**
         * Flushes the cached value.
         * Must be called with the {@code lock} variable held by this thread.
         */
        final void flush() {
            // NOTE: is only called from updateValue(Object) which holds the lock
            // assert(Thread.holdsLock(lock));
            isCached = false;
            exception = null;
            value = null;
        }
        /**
         * Gets the cached value.
         * If the value has not yet been parsed from the string value,
         * parse it now.
         * @return the parsed value, or null if there was no string value
         * @throws IllegalArgumentException if there was a problem
         */
        public T getValue() throws IllegalArgumentException {
            // Not quite double-check locking -- since isCached is marked as volatile
            if (!isCached) {
                synchronized (lock) {
                    try {
                        value = (stringValue == null) ? null : parse(stringValue);
                        exception = null;
                    } catch (Exception e) {
                        value = null;
                        exception = new IllegalArgumentException(e);
                    } finally {
                        isCached = true;
                    }
                }
            }
            if (exception != null) {
                throw exception;
            } else {
                return value;
            }
        }

        /**
         * Gets the cached value.
         * If the value has not yet been parsed from the string value,
         * parse it now.
         * If there is no string value, or there was a parse error,
         * returns the given default value.
         * @param defaultValue the value to return if there was a problem
         * @return the parsed value, or the default if there was no
         *    string value or a problem during parse
         */
        public T getValue(T defaultValue) {
            try {
                T result = getValue();
                return (result == null) ? defaultValue : result;
            } catch (IllegalArgumentException e) {
                return defaultValue;
            }
        }
        @Override
        public String toString() {
            if (!isCached) {
                return "{not cached}";
            } else if (exception != null) {
                return "{Exception: " + exception + "}";
            } else {
                return "{Value: " + value + "}";
            }
        }
        /**
         * Parse a string, converting it to an object of the value type.
         * @param rep the string representation to parse
         * @returns the parsed value
         * @throws Exception if the parse failed
         */
        protected abstract T parse(String rep) throws Exception;
    }

    private static final String[] TRUE_VALUES =  { "true",  "t", "yes", "y", "on"  };
    private static final String[] FALSE_VALUES = { "false", "f", "no",  "n", "off" };
    
    /*
     * Cached translated values
     */

    private CachedValue<Boolean> booleanValue = new CachedValue<Boolean>() {
        protected Boolean parse(String rep) throws IllegalArgumentException {
            for (int i = 0; i < TRUE_VALUES.length; i++){
                if (rep.equalsIgnoreCase(TRUE_VALUES[i])) {
                    return Boolean.TRUE;
                }
            }
            for (int i = 0; i < FALSE_VALUES.length; i++){
                if (rep.equalsIgnoreCase(FALSE_VALUES[i])) {
                    return Boolean.FALSE;
                }
            }
            throw new IllegalArgumentException();
        }
    };

    private CachedValue<String> cachedStringValue = new CachedValue<String>() {
        protected String parse(String rep) {
            return rep;
        }
    };

    private CachedValue<Integer> integerValue = new CachedValue<Integer>() {
        protected Integer parse(String rep) throws NumberFormatException {
            return Integer.valueOf(rep);
        }
    };

    private CachedValue<Long> longValue = new CachedValue<Long>() {
        protected Long parse(String rep) throws NumberFormatException {
            return Long.valueOf(rep);
        }
    };

    private CachedValue<Float> floatValue = new CachedValue<Float>() {
        protected Float parse(String rep) throws NumberFormatException {
            return Float.valueOf(rep);
        }
    };

    private CachedValue<Double> doubleValue = new CachedValue<Double>() {
        protected Double parse(String rep) throws NumberFormatException {
            return Double.valueOf(rep);
        }
    };

    
    private CachedValue<Class> classValue = new CachedValue<Class>() {
        protected Class parse(String rep) throws ClassNotFoundException {
            return Class.forName(rep);
        }
    };

    /*
     * Constructors
     */

    /**
     * Gets the DynamicProperty for a given property name.
     * This may be a previously constructed object,
     * or an object constructed on-demand to satisfy the request.
     * 
     * @param propName the name of the property
     * @return a DynamicProperty object that holds the cached value of
     *    the configuration property named {@code propName}
     */
    public static DynamicProperty getInstance(String propName) {
        // This is to ensure that a configuration source is registered with
        // DynamicProperty
        if (dynamicPropertySupportImpl == null) {
            DynamicPropertyFactory.getInstance();
        }
        DynamicProperty prop = ALL_PROPS.get(propName);
        if (prop == null) {
            prop = new DynamicProperty(propName);
            DynamicProperty oldProp = ALL_PROPS.putIfAbsent(propName, prop);
            if (oldProp != null) {
                prop = oldProp;
            }
        }
        return prop;
    }

    protected DynamicProperty() {        
    }
    /**
     * Create a new DynamicProperty with a given property name.
     *
     * @param propName the name of the property
     */
    private DynamicProperty(String propName) {
        this.propName = propName;
        updateValue();
    }

    /*
     * Accessor
     */

    /**
     * Gets the name of the property.
     */
    public String getName() {
        return propName;
    }

    /**
     * Gets the time (in milliseconds since the epoch)
     * when the property value was last set/changed.
     */
    public long getChangedTimestamp() {
        synchronized (lock) {
            return changedTime;
        }
    }

    /**
     * Gets the current value of the property as a String.
     *
     * @return the current property value, or null if there is none
     */
    public String getString() {
        return cachedStringValue.getValue();
    }

    /**
     * Gets the current value of the property as a String.
     *
     * @param defaultValue the value to return if the property is not defined
     * @return the current property value, or the default value there is none
     */
    public String getString(String defaultValue) {
        return cachedStringValue.getValue(defaultValue);
    }

    /**
     * Gets the current value of the property as an Boolean.
     * A property string value of "true", "yes", "on", "t" or "y"
     * produces {@code Boolean.TRUE}.
     * A property string value of "false", "no", "off", "f" or "b"
     * produces {@code Boolean.FALSE}.
     * (The value comparison ignores case.)
     * Any other value will result in an exception.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException if the property is defined but
     *    is not an Boolean
     */
    public Boolean getBoolean() throws IllegalArgumentException {
        return booleanValue.getValue();
    }

    /**
     * Gets the current value of the property as an Boolean.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not of the proper format
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Boolean getBoolean(Boolean defaultValue) {
        return booleanValue.getValue(defaultValue);
    }

    /**
     * Gets the current value of the property as an Integer.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException if the property is defined but
     *    is not an Integer
     */
    public Integer getInteger() throws IllegalArgumentException {
        return integerValue.getValue();
    }

    /**
     * Gets the current value of the property as an Integer.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not of the proper format
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Integer getInteger(Integer defaultValue) {
        return integerValue.getValue(defaultValue);
    }

    /**
     * Gets the current value of the property as a Float.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException if the property is defined but is
     *    not a Float
     */
    public Float getFloat() throws IllegalArgumentException {
        return floatValue.getValue();
    }

    /**
     * Gets the current value of the property as a Float.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not of the proper format
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Float getFloat(Float defaultValue) {
        return floatValue.getValue(defaultValue);
    }

    /**
     * Gets the current value of the property as a Long.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException if the property is defined but is
     *    not a Long
     */
    public Long getLong() throws IllegalArgumentException {
        return longValue.getValue();
    }

    /**
     * Gets the current value of the property as a Long.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not of the proper format
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Long getLong(Long defaultValue) {
        return longValue.getValue(defaultValue);
    }

    /**
     * Gets the current value of the property as a Long.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException if the property is defined but is
     *    not a Long
     */
    public Double getDouble() throws IllegalArgumentException {
        return doubleValue.getValue();
    }

    /**
     * Gets the current value of the property as a Long.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not of the proper format
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Double getDouble(Double defaultValue) {
        return doubleValue.getValue(defaultValue);
    }
    
    
    /**
     * Gets the current value of the property as a Class.
     *
     * @return the current property value, or null if there is none.
     * @throws IllegalArgumentException zif the property is defined but is
     *    not the name of a Class
     */
    public Class getNamedClass() throws IllegalArgumentException {
        return classValue.getValue();
    }

    /**
     * Gets the current value of the property as a Class.
     *
     * @param defaultValue the value to return if the property is not defined,
     *    or is not the name of a Class
     * @return the current property value, or the default value if there is
     *    none or the property is not of the proper format
     */
    public Class<?> getNamedClass(Class<?> defaultValue) {
        return classValue.getValue(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCachedValue(Class<T> objectType) {
        T result = null;
        try {
            if (Integer.class.equals(objectType)) {
                result = (T) integerValue.getValue();
            } else if (String.class.equals(objectType)) {
                result = (T) cachedStringValue.getValue();
            } else if (Boolean.class.equals(objectType)) {
                result = (T) booleanValue.getValue();
            } else if (Float.class.equals(objectType)) {
                result = (T) floatValue.getValue();
            } else if (Double.class.equals(objectType)) {
                result = (T) doubleValue.getValue();
            } else if (Long.class.equals(objectType)) {
                result = (T) longValue.getValue();
            } else if (Class.class.equals(objectType)) {
                result = (T) classValue.getValue();            
            }
        } catch (Exception e) {            
        }
        if (result == null) {
            return Optional.absent();
        } else {
            return Optional.of(result);
        }
    }
    
    /*
     * Updating the cached value
     */

    /**
     * Adds a callback to the DynamicProperty to run
     * when the value of the propety is updated.
     */
    public void addCallback(Runnable r) {
        if (r == null) {
            throw new NullPointerException("Cannot add null callback to DynamicProperty");
        }
        callbacks.add(r);
    }

    public void addValidator(PropertyChangeValidator validator) {
        if (validator == null) {
            throw new NullPointerException("Cannot add null validator to DynamicProperty");            
        }
        validators.add(validator);
    }
    
    /**
     * Removes a callback to the DynamicProperty so that it will
     * no longer be run when the value of the propety is updated.
     *
     * @return true iff the callback was previously registered
     */
    public boolean removeCallback(Runnable r) {
        return callbacks.remove(r);
    }
    
    Set<Runnable> getCallbacks() {
        return callbacks;         
    }

    private void notifyCallbacks() {
        for (Runnable r : callbacks) {
            try {
                r.run();
            } catch (Exception e) {
                logger.error("Error in DynamicProperty callback", e);
            }
        }
    }

    private void validate(String newValue) {
        for (PropertyChangeValidator v: validators) {
            try {
                v.validate(newValue);
            } catch (ValidationException e) {
                throw e;
            } catch (Throwable e) {
                throw new ValidationException("Unexpected exception during validation", e);
            }
        }
    }
    
    // return true iff the value actually changed
    private boolean updateValue() {
        String newValue;
        try {
            if (dynamicPropertySupportImpl != null) {
                newValue = dynamicPropertySupportImpl.getString(propName);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to update property: " + propName, e);
            return false;
        }
        return updateValue(newValue);
    }

    // return true iff the value actually changed
    boolean updateValue(Object newValue) {
        String nv = (newValue == null) ? null : newValue.toString();
        synchronized (lock) {
            if ((nv == null && stringValue == null)
               || (nv != null && nv.equals(stringValue))) {
                return false;
            }
            stringValue = nv;
            cachedStringValue.flush();
            booleanValue.flush();
            integerValue.flush();
            floatValue.flush();
            classValue.flush();
            doubleValue.flush();
            longValue.flush();
            changedTime = System.currentTimeMillis();
            return true;
        }
    }

    // return true iff the value actually changed
    private static boolean updateProperty(String propName, Object value) {
        DynamicProperty prop = ALL_PROPS.get(propName);
        if (prop != null && prop.updateValue(value)) {
            prop.notifyCallbacks();
            return true;
        }
        return false;
    }

    // return true iff _some_ value actually changed
    private static boolean updateAllProperties() {
        boolean changed = false;
        for (DynamicProperty prop : ALL_PROPS.values()) {
            if (prop.updateValue()) {
                prop.notifyCallbacks();
                changed = true;
            }
        }
        return changed;
    }
    
    private static void validate(String propName, Object value) {
        DynamicProperty prop = ALL_PROPS.get(propName);
        if (prop != null) {
            String newValue = (value == null)? null : value.toString();
            prop.validate(newValue);
        }
    }


    /**
     * A callback object that listens for configuration changes
     * and maintains cached property values.
     */
    static class DynamicPropertyListener implements PropertyListener {
        DynamicPropertyListener() { }
        @Override
        public void configSourceLoaded(Object source) {
            updateAllProperties();
        }
        @Override
        public void addProperty(Object source, String name, Object value, boolean beforeUpdate) {
            if (!beforeUpdate) {
                updateProperty(name, value);
            } else {
                validate(name, value);
            }
        }
        @Override
        public void setProperty(Object source, String name, Object value, boolean beforeUpdate) {
            if (!beforeUpdate) {
                updateProperty(name, value);
            } else {
                validate(name, value);
            }
        }
        @Override
        public void clearProperty(Object source, String name, Object value, boolean beforeUpdate) {
            if (!beforeUpdate) {
                updateProperty(name, value);
            }
        }
        @Override
        public void clear(Object source, boolean beforeUpdate) {
            if (!beforeUpdate) {
                updateAllProperties();
            }
        }
    }

    /**
     * Initialize the DynamicProperty capability by installing
     * a configuration listener.
     */
    static synchronized void initialize(DynamicPropertySupport config) {
        dynamicPropertySupportImpl = config;
        config.addConfigurationListener(new DynamicPropertyListener());
        updateAllProperties();
    }

    
    static void registerWithDynamicPropertySupport(DynamicPropertySupport config) {
        initialize(config);
    }
            
    /*
     * Object protocol
     */

    @Override
    public String toString() {
        return "{DynamicProperty:" + propName + "}";
    }

} 
