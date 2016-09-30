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

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyContainer;
import com.netflix.archaius.api.PropertyListener;
import com.netflix.archaius.property.ListenerManager.ListenerUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.Function;

/**
 * Implementation of PropertyContainer which reuses the same object for each
 * type.  This implementation assumes that each fast property is mostly accessed
 * as the same type but allows for additional types to be deserialized.  
 * Instead of incurring the overhead for caching in a hash map, the objects are 
 * stored in a CopyOnWriteArrayList and items are retrieved via a linear scan.
 * 
 * Once created a PropertyContainer property cannot be removed.  However, listeners may be
 * added and removed. 
 */
public class DefaultPropertyContainer implements PropertyContainer {
    private final Logger LOG = LoggerFactory.getLogger(DefaultPropertyContainer.class);
    
    enum CacheType {
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
        
        CacheType(Class<?> ... type) {
            types = type;
        }
        
        static CacheType fromClass(Class<?> clazz) {
            for (CacheType type : values()) {
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

    class PropertyWithDefault<T> implements Property<T> {
        private CachedProperty<T> delegate;
        private T defaultValue;

        public PropertyWithDefault(CachedProperty<T> delegate, T defaultValue) {
            this.defaultValue = defaultValue;
            this.delegate = delegate;
        }
        
        @Override
        public T get() {
            T value = delegate.get();
            return value != null ? value : defaultValue;
        }

        @Override
        public void addListener(PropertyListener<T> listener) {
            listeners.add(listener, new ListenerUpdater() {
                private AtomicReference<T> last = new AtomicReference<T>(null);
                
                @Override
                public void update() {
                    final T prev = last.get();
                    final T value;
                    
                    try {
                        value = get();
                    } catch (Exception e) {
                        listener.onParseError(e);
                        return;
                    }

                    if (prev != value && last.compareAndSet(prev, value)) {
                        listener.onChange(value != null ? value : defaultValue);
                    }
                }
            });
        }

        @Override
        public void removeListener(PropertyListener<T> listener) {
            listeners.remove(listener);
        }

        @Override
        public String getKey() {
            return delegate.getKey();
        }
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
            CachedProperty other = (CachedProperty) obj;
            return type != other.type;
        }
        private final AtomicStampedReference<T> cache = new AtomicStampedReference<>(null, -1);
        private final int type;
        
        CachedProperty(int type) {
            this.type = type;
        }
        
        public String getKey() {
            return DefaultPropertyContainer.this.key;
        }

        /**
         * Fetch the latest version of the property.  If not up to date then resolve to the latest
         * value, inline.
         * 
         * TODO: Make resolving property value an background task
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
                } catch (Exception e) {
                    LOG.warn("Unable to get current version of property '{}'", key, e);
                }
                
                if (cache.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                    // Slight race condition here but not important enough to warrent locking
                    lastUpdateTimeInMillis = System.currentTimeMillis();
                    return newValue;
                }
            }
            return cache.getReference();
        }
        
        public long getLastUpdateTime(TimeUnit units) {
            return units.convert(lastUpdateTimeInMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * Resolve to the most recent value
         * @return
         * @throws Exception
         */
        protected abstract T resolveCurrent() throws Exception;
    }

    /**
     * Add a new property to the end of the array list but first check
     * to see if it already exists.
     * @param newProperty
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> Property<T> add(final int type, final Function<Integer, CachedProperty<T>> creator, final T defaultValue) {
        do {
            Iterator<CachedProperty<?>> iter = cache.iterator();
            while (iter.hasNext()) {
                CachedProperty<?> element = iter.next();
                if (element.type == type) {
                    return new PropertyWithDefault<T>((CachedProperty<T>)element, defaultValue);
                }
            }
            
            // TODO(nikos): This while() looks like it's redundant
            // since we are only calling add() after a get().
            CachedProperty<T> cachedProperty = creator.apply(type);
            if (cache.addIfAbsent(cachedProperty)) {
                return new PropertyWithDefault<T>(cachedProperty, defaultValue);
            }
        } while (true);
    }
    
    @Override
    public Property<String> asString(final String defaultValue) {
        return add(
            CacheType.STRING.ordinal(), 
            type -> new CachedProperty<String>(type) {
                @Override
                protected String resolveCurrent() throws Exception {
                    return config.getString(key, null);
                }
            }, 
            defaultValue);
    }

    @Override
    public Property<Integer> asInteger(final Integer defaultValue) {
        return add(
            CacheType.INTEGER.ordinal(), 
            type -> new CachedProperty<Integer>(type) {
                @Override
                protected Integer resolveCurrent() throws Exception {
                    return config.getInteger(key, null);
                }
            }, 
            defaultValue);
        }

    @Override
    public Property<Long> asLong(final Long defaultValue) {
        return add(
            CacheType.LONG.ordinal(), 
            type -> new CachedProperty<Long>(type) {
                @Override
                protected Long resolveCurrent() throws Exception {
                    return config.getLong(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<Double> asDouble(final Double defaultValue) {
        return add(
            CacheType.DOUBLE.ordinal(), 
            type -> new CachedProperty<Double>(type) {
                @Override
                protected Double resolveCurrent() throws Exception {
                    return config.getDouble(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<Float> asFloat(final Float defaultValue) {
        return add(
            CacheType.FLOAT.ordinal(), 
            type -> new CachedProperty<Float>(type) {
                @Override
                protected Float resolveCurrent() throws Exception {
                    return config.getFloat(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<Short> asShort(final Short defaultValue) {
        return add(
            CacheType.SHORT.ordinal(), 
            type -> new CachedProperty<Short>(type) {
                @Override
                protected Short resolveCurrent() throws Exception {
                    return config.getShort(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<Byte> asByte(final Byte defaultValue) {
        return add(
            CacheType.BYTE.ordinal(), 
            type -> new CachedProperty<Byte>(type) {
                @Override
                protected Byte resolveCurrent() throws Exception {
                    return config.getByte(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<BigDecimal> asBigDecimal(final BigDecimal defaultValue) {
        return add(
            CacheType.BIG_DECIMAL.ordinal(), 
            type -> new CachedProperty<BigDecimal>(type) {
                @Override
                protected BigDecimal resolveCurrent() throws Exception {
                    return config.getBigDecimal(key, null);
                }
            },
            defaultValue);
    }
    
    @Override
    public Property<Boolean> asBoolean(final Boolean defaultValue) {
        return add(
            CacheType.BOOLEAN.ordinal(), 
            type -> new CachedProperty<Boolean>(type) {
                @Override
                protected Boolean resolveCurrent() throws Exception {
                    return config.getBoolean(key, null);
                }
            },
            defaultValue);
    }

    @Override
    public Property<BigInteger> asBigInteger(final BigInteger defaultValue) {
        return add(
            CacheType.BIG_INTEGER.ordinal(),
            type -> new CachedProperty<BigInteger>(type) {
                @Override
                protected BigInteger resolveCurrent() throws Exception {
                    return config.getBigInteger(key, null);
                }
            },
            defaultValue);
    }
    
    /**
     * No caching for custom types.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Property<T> asType(final Class<T> type, final T defaultValue) {
        switch (CacheType.fromClass(type)) {
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
        default:
            return add(
                CacheType.CUSTOM.ordinal(),
                t -> new CachedProperty<T>(t) {
                    @Override
                    protected T resolveCurrent() throws Exception {
                        return config.get(type, key, null);
                    }
                },
                defaultValue);
        }
    }

    @Override
    public <T> Property<T> asType(Function<String, T> type, String defaultValue) {
        return add(
            CacheType.CUSTOM.ordinal(),
            t -> new CachedProperty<T>(t) {
                @Override
                protected T resolveCurrent() throws Exception {
                    return type.apply(config.getString(key, defaultValue));
                }
            },
            type.apply(defaultValue));
    }
}
