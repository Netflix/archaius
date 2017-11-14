package com.netflix.archaius;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyContainer;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyListener;

public class DefaultPropertyFactory implements PropertyFactory, ConfigListener {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPropertyFactory.class);
    
    /**
     * Create a Property factory that is attached to a specific config
     * @param config
     * @return
     */
    public static DefaultPropertyFactory from(final Config config) {
        return new DefaultPropertyFactory(config);
    }

    /**
     * Config from which properties are retrieved.  Config may be a composite.
     */
    private final Config config;
    
    /**
     * Cache of properties so PropertyContainer may be re-used
     */
    private final ConcurrentMap<KeyAndType<?>, Property<?>> properties = new ConcurrentHashMap<>();
    
    /**
     * Monotonically incrementing version number whenever a change in the Config
     * is identified.  This version is used as a global dirty flag indicating that
     * properties should be updated when fetched next.
     */
    private final AtomicInteger masterVersion = new AtomicInteger();
    
    /**
     * Array of all active callbacks.  ListenerWrapper#update will be called for any
     * change in config.  
     */
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    
    public DefaultPropertyFactory(Config config) {
        this.config = config;
        this.config.addListener(this);
    }

    @Override
    public PropertyContainer getProperty(String propName) {
        return new PropertyContainer() {
            @Override
            public Property<String> asString(String defaultValue) {
                return get(propName, String.class).orElse(defaultValue);
            }

            @Override
            public Property<Integer> asInteger(Integer defaultValue) {
                return get(propName, Integer.class).orElse(defaultValue);
            }

            @Override
            public Property<Long> asLong(Long defaultValue) {
                return get(propName, Long.class).orElse(defaultValue);
            }

            @Override
            public Property<Double> asDouble(Double defaultValue) {
                return get(propName, Double.class).orElse(defaultValue);
            }

            @Override
            public Property<Float> asFloat(Float defaultValue) {
                return get(propName, Float.class).orElse(defaultValue);
            }

            @Override
            public Property<Short> asShort(Short defaultValue) {
                return get(propName, Short.class).orElse(defaultValue);
            }

            @Override
            public Property<Byte> asByte(Byte defaultValue) {
                return get(propName, Byte.class).orElse(defaultValue);
            }

            @Override
            public Property<Boolean> asBoolean(Boolean defaultValue) {
                return get(propName, Boolean.class).orElse(defaultValue);
            }

            @Override
            public Property<BigDecimal> asBigDecimal(BigDecimal defaultValue) {
                return get(propName, BigDecimal.class).orElse(defaultValue);
            }

            @Override
            public Property<BigInteger> asBigInteger(BigInteger defaultValue) {
                return get(propName, BigInteger.class).orElse(defaultValue);
            }

            @Override
            public <T> Property<T> asType(Class<T> type, T defaultValue) {
                return get(propName, type).orElse(defaultValue);
            }

            @Override
            public <T> Property<T> asType(Function<String, T> mapper, String defaultValue) {
                T typedDefaultValue = mapper.apply(defaultValue);
                return getFromSupplier(propName, null, () -> {
                    String value = config.getString(propName, null);
                    if (value != null) {
                        try {
                            return mapper.apply(value);
                        } catch (Exception e) {
                            LOG.warn("Invalid value '{}' for property '{}'", propName, value);
                        }
                    }
                    
                    return typedDefaultValue;
                });
            }
        };
    }
    
    @Override
    public void onConfigAdded(Config config) {
        invalidate();
    }

    @Override
    public void onConfigRemoved(Config config) {
        invalidate();
    }

    @Override
    public void onConfigUpdated(Config config) {
        invalidate();
    }

    @Override
    public void onError(Throwable error, Config config) {
        // TODO
    }

    public void invalidate() {
        // Incrementing the version will cause all PropertyContainer instances to invalidate their
        // cache on the next call to get
        masterVersion.incrementAndGet();
        
        // We expect a small set of callbacks and invoke all of them whenever there is any change
        // in the configuration regardless of change. The blanket update is done since we don't track
        // a dependency graph of replacements.
        listeners.forEach(Runnable::run);
    }
    
    protected Config getConfig() {
        return this.config;
    }

    @Override
    public <T> Property<T> get(String key, Class<T> type) {
        return getFromSupplier(key, type, () -> config.get(type, key, null));
    }
    
    private <T> Property<T> getFromSupplier(String key, Class<T> type, Supplier<T> supplier) {
        return getFromSupplier(new KeyAndType<T>(key, type), supplier);
    }
        
    @SuppressWarnings("unchecked")
    private <T> Property<T> getFromSupplier(KeyAndType<T> keyAndType, Supplier<T> supplier) {
        return (Property<T>) properties.computeIfAbsent(keyAndType, (ignore) -> new PropertyImpl<T>(keyAndType, supplier));
    }
    
    private class PropertyImpl<T> implements Property<T> {
        private final KeyAndType<T> keyAndType;
        private final Supplier<T> supplier;
        private final AtomicStampedReference<T> cache = new AtomicStampedReference<>(null, -1);
        private final ConcurrentMap<PropertyListener<?>, Subscription> oldSubscriptions = new ConcurrentHashMap<>();
        
        public PropertyImpl(KeyAndType<T> keyAndType, Supplier<T> supplier) {
            this.keyAndType = keyAndType;
            this.supplier = supplier;
        }
        
        @Override
        public T get() {
            int cacheVersion = cache.getStamp();
            int latestVersion  = masterVersion.get();
            
            if (cacheVersion != latestVersion) {
                T currentValue = cache.getReference();
                T newValue = null;
                try {
                    newValue = supplier.get();
                } catch (Exception e) {
                    LOG.warn("Unable to get current version of property '{}'", keyAndType.key, e);
                }
                
                if (cache.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                    // Slight race condition here but not important enough to warrent locking
                    return newValue;
                }
            }
            return cache.getReference();
        }

        @Override
        public String getKey() {
            return keyAndType.key;
        }
        
        @Override
        public Subscription onChange(Consumer<T> consumer) {
            Runnable action = new Runnable() {
                private T current = get();
                @Override
                public synchronized void run() {
                    T newValue = get();
                    if (current == newValue && current == null) {
                        return;
                    } else if (current == null) {
                        current = newValue;
                    } else if (newValue == null) {
                        current = null;
                    } else if (current.equals(newValue)) {
                        return;
                    } else {
                        current = newValue;
                    }
                    consumer.accept(current);
                }
            };
            
            listeners.add(action);
            return () -> listeners.remove(action);
        }

        @Deprecated
        public void addListener(PropertyListener<T> listener) {
            Subscription cancel = onChange(new Consumer<T>() {
                @Override
                public void accept(T t) {
                    listener.accept(t);
                }
            });
            oldSubscriptions.put(listener, cancel);
        }

        /**
         * Remove a listener previously registered by calling addListener
         * @param listener
         */
        @Deprecated
        public void removeListener(PropertyListener<T> listener) {
            Optional.ofNullable(oldSubscriptions.remove(listener)).ifPresent(Subscription::unsubscribe);
        }
        
        @Override
        public Property<T> orElse(T defaultValue) {
            return new PropertyImpl<T>(keyAndType, () -> Optional.ofNullable(supplier.get()).orElse(defaultValue));
        }
        
        @Override
        public Property<T> orElseGet(String key) {
            if (!keyAndType.hasType()) {
                throw new IllegalStateException("Type information lost due to map() operation.  All calls to orElse[Get] must be made prior to calling map");
            }
            KeyAndType<T> keyAndType = this.keyAndType.withKey(key);
            Property<T> next = DefaultPropertyFactory.this.get(key, keyAndType.type);
            return new PropertyImpl<T>(keyAndType, () -> Optional.ofNullable(supplier.get()).orElseGet(next));
        }
        
        @Override
        public <S> Property<S> map(Function<T, S> mapper) {
            return new PropertyImpl<S>(keyAndType.discardType(), () -> mapper.apply(supplier.get()));
        }
    }
    
    private static final class KeyAndType<T> {
        private final String key;
        private final Class<T> type;

        public KeyAndType(String key, Class<T> type) {
            this.key = key;
            this.type = type;
        }

        public <S> KeyAndType<S> discardType() {
            return new KeyAndType<S>(key, null);
        }

        public KeyAndType<T> withKey(String newKey) {
            return new KeyAndType<T>(newKey, type);
        }
        
        public boolean hasType() {
            return type != null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
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
            KeyAndType other = (KeyAndType) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }
}
