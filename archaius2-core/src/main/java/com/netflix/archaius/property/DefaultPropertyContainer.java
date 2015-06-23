/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius.property;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.Config;
import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyContainer;
import com.netflix.archaius.PropertyListener;
import com.netflix.archaius.property.ListenerManager.ListenerUpdater;

/**
 * Implementation of PropertyContainer which reuses the same object for each
 * type.  This implementation assumes that each fast property is mostly accessed
 * as the same type but allows for additional types to be deserialized.  
 * Instead of incurring the overhead for caching in a hash map, the objects are 
 * stored in a CopyOnWriteArrayList and items are retrieved via a linear scan.
 * 
 * Once created a PropertyContainer property cannot be removed.  However, listeners may be
 * added and removed. 
 * 
 * @author elandau
 *
 */
public class DefaultPropertyContainer implements PropertyContainer {
    private final Logger LOG = LoggerFactory.getLogger(DefaultPropertyContainer.class);
    
    enum Type {
        INTEGER     (int.class,     Integer.class),
        BYTE        (byte.class,    Byte.class),
        SHORT       (short.class,   Short.class),
        DOUBLE      (double.class,  Double.class),
        FLOAT       (float.class,   Float.class),
        BOOLEAN     (boolean.class, Boolean.class),
        LONG        (long.class,    Long.class),
        STRING      (String.class),
        BIG_DECIMAL (BigDecimal.class),
        BIG_INTEGER (BigInteger.class),
        CUSTOM      ();     // Must be last
        
        private final Class<?>[] types;
        
        Type(Class<?> ... type) {
            types = type;
        }
        
        static Type fromClass(Class<?> clazz) {
            for (Type type : values()) {
                for (Class<?> cls : type.types) {
                    if (cls.equals(clazz)) {
                        return type;
                    }
                }
            }
            return CUSTOM;
        }
    }

    /**
     * The property name
     */
    private final String key;
    
    /**
     * Config from which property values are resolved
     */
    private final Config config;
    
    /**
     * Cache for each type attached to this property.  
     */
    private final CopyOnWriteArrayList<CachedProperty<?>> cache = new CopyOnWriteArrayList<CachedProperty<?>>();
    
    /**
     * Listeners are tracked globally as an optimization so it is not necessary to iterate through all 
     * property containers when the listeners need to be invoked since the expectation is to have far
     * less listeners than property containers.
     */
    private final ListenerManager listeners;
    
    /**
     * Reference to the externally managed master version used as the dirty flag
     */
    private final AtomicInteger masterVersion;
    
    private volatile long lastUpdateTimeInMillis = 0;
    
    public DefaultPropertyContainer(String key, Config config, AtomicInteger version, ListenerManager listeners) {
        this.key = key;
        this.config = config;
        this.listeners = listeners;
        this.masterVersion = version;
    }

    abstract class CachedProperty<T> {
        @Override
        public int hashCode() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            @SuppressWarnings("rawtypes")
            CachedProperty other = (CachedProperty) obj;
            if (!getOuter().equals(other.getOuter()))
                return false;
            return (type == other.type);
        }

        private final AtomicStampedReference<T> cache = new AtomicStampedReference<>(null, -1);
        private final int type;
        
        CachedProperty(Type type) {
            this.type = type.ordinal();
        }
        
        public void addListener(final PropertyListener<T> listener, final T defaultValue) {
            listeners.add(listener, new ListenerUpdater() {
                private AtomicReference<T> last = new AtomicReference<T>(null);
                
                @Override
                public void update() {
                    final T prev = last.get();
                    final T value;
                    
                    try {
                        value = get(defaultValue);
                    }
                    catch (Exception e) {
                        listener.onParseError(e);
                        return;
                    }

                    if (prev != value) {
                        if (last.compareAndSet(prev, value)) {
                            listener.onChange(value);
                        }
                    }
                }
            });
        }
        
        public void removeListener(PropertyListener<T> listener) {
            listeners.remove(listener);
        }
        
        /**
         * Update to the latest value and return either the new value or previous value if not updated
         * or failed to resolve.
         * 
         * @param latestVersion
         * @return
         */
        T updateAndGet(int latestVersion) {
            int[] cacheVersion = new int[1];
            T existing = cache.get(cacheVersion);
            try {
                T value = resolveCurrent();
                if (value != existing && (value == null || existing == null || !existing.equals(value))) {
                    if (cache.compareAndSet(existing, value, cacheVersion[0], latestVersion)) {
                        // Slight race condition here but not important enough to warrent locking
                        lastUpdateTimeInMillis = System.currentTimeMillis();
                        return value;
                    }
                    else {
                        cache.getReference();
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Unable to get current version of property '{}'. Error: {}", key, e.getMessage());
            }
            return existing;
        }
        
        /**
         * Fetch the latest version of the property.  If not up to date then resolve to the latest
         * value, inline.
         * 
         * TODO: Make resolving property value an offline task
         * 
         * @return
         */
        public T get(T defaultValue) {
            int cacheVersion = cache.getStamp();
            int latestVersion  = masterVersion.get();
            
            T value;
            if (cacheVersion != latestVersion) {
                value = updateAndGet(latestVersion);
            }
            else {
                value = cache.getReference();
            }
            return value != null ? value : defaultValue;
        }
        
        public long getLastUpdateTime(TimeUnit units) {
            return units.convert(lastUpdateTimeInMillis, TimeUnit.MILLISECONDS);
        }

        private DefaultPropertyContainer getOuter() {
            return DefaultPropertyContainer.this;
        }
        
        /**
         * Resolve to the most recent value
         * @return
         * @throws Exception
         */
        protected abstract T resolveCurrent() throws Exception;
    }

    /**
     * One of these is created every time a Property object is created from 
     * PropertyFactory in client code.  A common object tracks each property
     * but each instance in client code may use a different defaultValue.
     * @author elandau
     *
     * @param <T>
     */
    class AbstractProperty<T> implements Property<T>  {

        private final CachedProperty<T> delegate;
        private final T defaultValue;

        public AbstractProperty(CachedProperty<T> delegate, T defaultValue) {
            this.delegate = delegate;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public T get() {
            return delegate.get(defaultValue);
        }

        @Override
        public long getLastUpdateTime(TimeUnit units) {
            return delegate.getLastUpdateTime(units);
        }

        @Override
        public void unsubscribe() {
            // TODO:
        }

        @Override
        public Property<T> addListener(PropertyListener<T> listener) {
            delegate.addListener(listener, defaultValue);
            return this;
        }

        @Override
        public void removeListener(PropertyListener<T> listener) {
            delegate.removeListener(listener);
        }
    }
    
    /**
     * Add a new property to the end of the array list but first check
     * to see if it already exists.
     * @param newProperty
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> CachedProperty<T> add(CachedProperty<T> newProperty) {
        while (!cache.addIfAbsent(newProperty)) {
            for (CachedProperty<?> property : cache) {
                if (property.type == newProperty.type) {
                    return (CachedProperty<T>) property;
                }
            }
        }
        
        return newProperty;
    }
    
    /**
     * Retrieve a cached instance of the fast property for the specified
     * primitive data type
     * @param type
     * @return Cached type or null if does not exist
     */
    @SuppressWarnings("unchecked")
    private <T> CachedProperty<T> get(int type) {
        for (CachedProperty<?> property : cache) {
            if (property.type == type) {
                return (CachedProperty<T>) property;
            }
        }
        return null;
    }
    
    @Override
    public Property<String> asString(String  defaultValue) {
        CachedProperty<String> prop = get(Type.STRING.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<String>(Type.STRING) {
                @Override
                protected String resolveCurrent() throws Exception {
                    return config.getString(key, null);
                }
            });
        }
        return new AbstractProperty<String>(prop, defaultValue);
    }

    @Override
    public Property<Integer> asInteger(Integer defaultValue) {
        CachedProperty<Integer> prop = get(Type.INTEGER.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Integer>(Type.INTEGER) {
                @Override
                protected Integer resolveCurrent() throws Exception {
                    return config.getInteger(key, null);
                }
            });
        }
        return new AbstractProperty<Integer>(prop, defaultValue);
    }

    @Override
    public Property<Long> asLong(Long defaultValue) {
        CachedProperty<Long> prop = get(Type.LONG.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Long>(Type.LONG) {
                @Override
                protected Long resolveCurrent() throws Exception {
                    return config.getLong(key, null);
                }
            });
        }
        return new AbstractProperty<Long>(prop, defaultValue);
    }

    @Override
    public Property<Double> asDouble(Double defaultValue) {
        CachedProperty<Double> prop = get(Type.DOUBLE.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Double>(Type.DOUBLE) {
                @Override
                protected Double resolveCurrent() throws Exception {
                    return config.getDouble(key, null);
                }
            });
        }
        return new AbstractProperty<Double>(prop, defaultValue);
    }

    @Override
    public Property<Float> asFloat(Float defaultValue) {
        CachedProperty<Float> prop = get(Type.FLOAT.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Float>(Type.FLOAT) {
                @Override
                protected Float resolveCurrent() throws Exception {
                    return config.getFloat(key, null);
                }
            });
        }
        return new AbstractProperty<Float>(prop, defaultValue);
    }

    @Override
    public Property<Short> asShort(Short defaultValue) {
        CachedProperty<Short> prop = get(Type.SHORT.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Short>(Type.SHORT) {
                @Override
                protected Short resolveCurrent() throws Exception {
                    return config.getShort(key, null);
                }
            });
        }
        return new AbstractProperty<Short>(prop, defaultValue);
    }

    @Override
    public Property<Byte> asByte(Byte defaultValue) {
        CachedProperty<Byte> prop = get(Type.BYTE.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Byte>(Type.BYTE) {
                @Override
                protected Byte resolveCurrent() throws Exception {
                    return config.getByte(key, null);
                }
            });
        }
        return new AbstractProperty<Byte>(prop, defaultValue);
    }

    @Override
    public Property<BigDecimal> asBigDecimal(BigDecimal defaultValue) {
        CachedProperty<BigDecimal> prop = get(Type.BIG_DECIMAL.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<BigDecimal>(Type.BIG_DECIMAL) {
                @Override
                protected BigDecimal resolveCurrent() throws Exception {
                    return config.getBigDecimal(key, null);
                }
            });
        }
        return new AbstractProperty<BigDecimal>(prop, defaultValue);
    }
    
    @Override
    public Property<Boolean> asBoolean(Boolean defaultValue) {
        CachedProperty<Boolean> prop = get(Type.BOOLEAN.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<Boolean>(Type.BOOLEAN) {
                @Override
                protected Boolean resolveCurrent() throws Exception {
                    return config.getBoolean(key, null);
                }
            });
        }
        return new AbstractProperty<Boolean>(prop, defaultValue);
    }

    @Override
    public Property<BigInteger> asBigInteger(BigInteger defaultValue) {
        CachedProperty<BigInteger> prop = get(Type.BIG_INTEGER.ordinal());
        if (prop == null) {
            prop = add(new CachedProperty<BigInteger>(Type.BIG_INTEGER) {
                @Override
                protected BigInteger resolveCurrent() throws Exception {
                    return config.getBigInteger(key, null);
                }
            });
        }
        return new AbstractProperty<BigInteger>(prop, defaultValue);
    }

    /**
     * No caching for custom types.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Property<T> asType(final Class<T> type, T defaultValue) {
        switch (Type.fromClass(type)) {
        case INTEGER:
            return (Property<T>) asInteger((Integer)defaultValue);
        case BYTE:
            return (Property<T>) asByte((Byte)defaultValue);
        case SHORT:
            return (Property<T>) asShort((Short)defaultValue);
        case DOUBLE:
            return (Property<T>) asDouble((Double)defaultValue);
        case FLOAT:
            return (Property<T>) asFloat((Float)defaultValue);
        case BOOLEAN:
            return (Property<T>) asBoolean((Boolean)defaultValue);
        case STRING:
            return (Property<T>) asString((String)defaultValue);
        case LONG:
            return (Property<T>) asLong((Long)defaultValue);
        case BIG_DECIMAL:
            return (Property<T>) asBigDecimal((BigDecimal)defaultValue);
        case BIG_INTEGER:
            return (Property<T>) asBigInteger((BigInteger)defaultValue);
        default: {
                CachedProperty<T> prop = add(new CachedProperty<T>(Type.CUSTOM) {
                    @Override
                    protected T resolveCurrent() throws Exception {
                        return config.get(type, key);
                    }
                });
                return new AbstractProperty<T>(prop, defaultValue);
            }
        }
    }
}
