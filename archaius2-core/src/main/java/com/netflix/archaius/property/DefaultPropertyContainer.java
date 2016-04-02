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

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyContainer;
import com.netflix.archaius.api.PropertyListener;
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

    abstract class CachedProperty<T> implements Property<T> {

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
            result = prime * result + type;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CachedProperty other = (CachedProperty) obj;
            if (defaultValue == null) {
                if (other.defaultValue != null)
                    return false;
            } else if (!defaultValue.equals(other.defaultValue))
                return false;
            if (type != other.type)
                return false;
            return true;
        }
        private final AtomicStampedReference<T> cache = new AtomicStampedReference<>(null, -1);
        private final int type;
        private final T defaultValue;
        
        CachedProperty(Type type, T defaultValue) {
            this.type = type.ordinal();
            this.defaultValue = defaultValue;
        }
        
        public void addListener(final PropertyListener<T> listener) {
            listeners.add(listener, new ListenerUpdater() {
                private AtomicReference<T> last = new AtomicReference<T>(null);
                
                @Override
                public void update() {
                    final T prev = last.get();
                    final T value;
                    
                    try {
                        value = get();
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
        
        @Override
        public String getKey() {
            return DefaultPropertyContainer.this.key;
        }

        public void removeListener(PropertyListener<T> listener) {
            listeners.remove(listener);
        }
        
        /**
         * Fetch the latest version of the property.  If not up to date then resolve to the latest
         * value, inline.
         * 
         * TODO: Make resolving property value an offline task
         * 
         * @return
         */
        public T get() {
            int cacheVersion = cache.getStamp();
            int latestVersion  = masterVersion.get();
            
            if (cacheVersion != latestVersion) {
                T currentValue = cache.getReference();
                T newValue = null;
                try {
                    newValue = resolveCurrent();
                }
                catch (Exception e) {
                    LOG.warn("Unable to get current version of property '{}'. Error: {}", key, e);
                }
                
                if (cache.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                    // Slight race condition here but not important enough to warrent locking
                    lastUpdateTimeInMillis = System.currentTimeMillis();
                    return firstNonNull(newValue, defaultValue);
                }
            }
            return firstNonNull(cache.getReference(), defaultValue);
        }
        
        public long getLastUpdateTime(TimeUnit units) {
            return units.convert(lastUpdateTimeInMillis, TimeUnit.MILLISECONDS);
        }

        private T firstNonNull(T first, T second) {
            return first == null ? second : first;
        }
        /**
         * Resolve to the most recent value
         * @return
         * @throws Exception
         */
        protected abstract T resolveCurrent() throws Exception;

        private DefaultPropertyContainer getOuterType() {
            return DefaultPropertyContainer.this;
        }
    }

    /**
     * Add a new property to the end of the array list but first check
     * to see if it already exists.
     * @param newProperty
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> CachedProperty<T> add(final CachedProperty<T> newProperty) {
        // TODO(nikos): This while() looks like it's redundant
        // since we are only calling add() after a get().
        while (!cache.add(newProperty)) {
            for (CachedProperty<?> property : cache) {
                if (property.equals(newProperty)) {
                    return (CachedProperty<T>) property;
                }
            }
        }
        
        return newProperty;
    }
    
    @Override
    public Property<String> asString(final String defaultValue) {
        return add(new CachedProperty<String>(Type.STRING, defaultValue) {
            @Override
            protected String resolveCurrent() throws Exception {
                return config.getString(key, null);
            }
        });
    }

    @Override
    public Property<Integer> asInteger(final Integer defaultValue) {
        return add(new CachedProperty<Integer>(Type.INTEGER, defaultValue) {
            @Override
            protected Integer resolveCurrent() throws Exception {
                return config.getInteger(key, null);
            }
        });
    }

    @Override
    public Property<Long> asLong(final Long defaultValue) {
        return add(new CachedProperty<Long>(Type.LONG, defaultValue) {
            @Override
            protected Long resolveCurrent() throws Exception {
                return config.getLong(key, null);
            }
        });
    }

    @Override
    public Property<Double> asDouble(final Double defaultValue) {
        return add(new CachedProperty<Double>(Type.DOUBLE, defaultValue) {
            @Override
            protected Double resolveCurrent() throws Exception {
                return config.getDouble(key, null);
            }
        });
    }

    @Override
    public Property<Float> asFloat(final Float defaultValue) {
        return add(new CachedProperty<Float>(Type.FLOAT, defaultValue) {
            @Override
            protected Float resolveCurrent() throws Exception {
                return config.getFloat(key, null);
            }
        });
    }

    @Override
    public Property<Short> asShort(final Short defaultValue) {
        return add(new CachedProperty<Short>(Type.SHORT, defaultValue) {
            @Override
            protected Short resolveCurrent() throws Exception {
                return config.getShort(key, null);
            }
        });
    }

    @Override
    public Property<Byte> asByte(final Byte defaultValue) {
        return add(new CachedProperty<Byte>(Type.BYTE, defaultValue) {
            @Override
            protected Byte resolveCurrent() throws Exception {
                return config.getByte(key, defaultValue);
            }
        });
    }

    @Override
    public Property<BigDecimal> asBigDecimal(final BigDecimal defaultValue) {
        return add(new CachedProperty<BigDecimal>(Type.BIG_DECIMAL, defaultValue) {
            @Override
            protected BigDecimal resolveCurrent() throws Exception {
                return config.getBigDecimal(key, defaultValue);
            }
        });
    }
    
    @Override
    public Property<Boolean> asBoolean(final Boolean defaultValue) {
        return add(new CachedProperty<Boolean>(Type.BOOLEAN, defaultValue) {
            @Override
            protected Boolean resolveCurrent() throws Exception {
                return config.getBoolean(key, defaultValue);
            }
        });
    }

    @Override
    public Property<BigInteger> asBigInteger(final BigInteger defaultValue) {
        return add(new CachedProperty<BigInteger>(Type.BIG_INTEGER, defaultValue) {
            @Override
            protected BigInteger resolveCurrent() throws Exception {
                return config.getBigInteger(key, defaultValue);
            }
        });
    }
    
    /**
     * No caching for custom types.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Property<T> asType(final Class<T> type, final T defaultValue) {
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
                CachedProperty<T> prop = add(new CachedProperty<T>(Type.CUSTOM, defaultValue) {
                    @Override
                    protected T resolveCurrent() throws Exception {
                        return config.get(type, key, defaultValue);
                    }
                });
                return prop;
            }
        }
    }
}
