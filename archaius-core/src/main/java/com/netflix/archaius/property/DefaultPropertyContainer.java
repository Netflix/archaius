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

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.Config;
import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyContainer;
import com.netflix.archaius.PropertyListener;

/**
 * Implementation of PropertyContainer which reuses the same object for each
 * type.  This implementation is assumes that each fast property is mostly accessed
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

    private final String key;
    private final Config config;
    private final CopyOnWriteArrayList<CachedProperty<?>> cache = new CopyOnWriteArrayList<CachedProperty<?>>();
    private volatile long lastUpdateTimeInMillis = 0;
    
    public DefaultPropertyContainer(String key, Config config) {
        this.key = key;
        this.config = config;
    }

    @Override
    public synchronized void update() {
        boolean didUpdate = false;
        for (CachedProperty<?> property : cache) {
            didUpdate |= property.update();
        }
        if (didUpdate)  
            lastUpdateTimeInMillis = System.currentTimeMillis();
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
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            return (type == other.type);
        }

        private volatile T existing = null;
        private CopyOnWriteArraySet<PropertyListener<T>> listeners = new CopyOnWriteArraySet<PropertyListener<T>>();
        private final int type;
        
        CachedProperty(Type type) {
            this.type = type.ordinal();
        }
        
        public void addListener(PropertyListener<T> listener) {
            this.listeners.add(listener);
        }
        
        public void removeListener(PropertyListener<T> listener) {
            this.listeners.remove(listener);
        }
        
        boolean update() {
            try {
                T next = getCurrent();
                if ((next == null || existing == null) && next == existing) {
                    return false;
                }
                else if (next == null || existing == null || !existing.equals(next)) {
                    existing = next;
                    // TODO: We need a plugable concurrency model here
                    for (PropertyListener<T> observer : listeners) {
                        observer.onChange(existing);
                    }
                    return true;
                }
            }
            catch (Exception e) {
                LOG.warn("Unable to get current version of property '{}'. Error: {}", key, e.getMessage());
                // TODO: We need a plugable concurrency model here
                for (PropertyListener<T> observer : listeners) {
                    observer.onParseError(e);
                }
            }
            return false;
        }
        
        public T get() {
            return existing;
        }
        
        public long getLastUpdateTime(TimeUnit units) {
            return units.convert(lastUpdateTimeInMillis, TimeUnit.MILLISECONDS);
        }

        private DefaultPropertyContainer getOuterType() {
            return DefaultPropertyContainer.this;
        }
        
        protected abstract T getCurrent() throws Exception;
    }

    class AbstractProperty<T> implements Property<T>  {

        private final CachedProperty<T> delegate;
        private final T defaultValue;

        public AbstractProperty(CachedProperty<T> delegate, T defaultValue) {
            this.delegate = delegate;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public T get() {
            T value = delegate.get();
            return value == null ? defaultValue : value;
        }

        @Override
        public long getLastUpdateTime(TimeUnit units) {
            return delegate.getLastUpdateTime(units);
        }

        @Override
        public void unsubscribe() {
        }

        @Override
        public Property<T> addListener(PropertyListener<T> listener) {
            delegate.addListener(listener);
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
        
        newProperty.update();
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
                protected String getCurrent() throws Exception {
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
                protected Integer getCurrent() throws Exception {
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
                protected Long getCurrent() throws Exception {
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
                protected Double getCurrent() throws Exception {
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
                protected Float getCurrent() throws Exception {
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
                protected Short getCurrent() throws Exception {
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
                protected Byte getCurrent() throws Exception {
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
                protected BigDecimal getCurrent() throws Exception {
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
                protected Boolean getCurrent() throws Exception {
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
                protected BigInteger getCurrent() throws Exception {
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
    public <T> Property<T> asType(Class<T> type, T defaultValue) {
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
                final Constructor<T> constructor;
                try {
                    constructor = type.getConstructor(String.class);
                    if (constructor != null) {
                        CachedProperty<T> prop = add(new CachedProperty<T>(Type.CUSTOM) {
                            @Override
                            protected T getCurrent() throws Exception {
                                String value = config.getString(key);
                                if (value == null) {
                                    return null;
                                }
                                else { 
                                    return constructor.newInstance(value);
                                }
                            }
                        });
                        return new AbstractProperty<T>(prop, defaultValue);
                    }
                } catch (NoSuchMethodException e) {
                } catch (SecurityException e) {
                    throw new UnsupportedOperationException("No parser for type " + type.getName(), e);
                }
              
                throw new UnsupportedOperationException("No parser for type " + type.getName());
            }
        }
    }
}
