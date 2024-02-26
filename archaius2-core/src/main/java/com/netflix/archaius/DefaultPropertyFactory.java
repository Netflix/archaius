package com.netflix.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyContainer;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Override
    public <T> Property<T> get(String key, Type type) {
        return getFromSupplier(key, type, () -> config.get(type, key, null));
    }

    private <T> Property<T> getFromSupplier(String key, Type type, Supplier<T> supplier) {
        return getFromSupplier(new KeyAndType<>(key, type), supplier);
    }

    @SuppressWarnings("unchecked")
    private <T> Property<T> getFromSupplier(KeyAndType<T> keyAndType, Supplier<T> supplier) {
        return (Property<T>) properties.computeIfAbsent(keyAndType, (ignore) -> new PropertyImpl<>(keyAndType, supplier));
    }

    private final class PropertyImpl<T> implements Property<T> {
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
                    // Possible race condition here but not important enough to warrant locking
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
        public Subscription subscribe(Consumer<T> consumer) {
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
        @Override
        public void addListener(PropertyListener<T> listener) {
            oldSubscriptions.put(listener, onChange(listener));
        }

        /**
         * Remove a listener previously registered by calling addListener
         * @param listener
         */
        @Deprecated
        @Override
        public void removeListener(PropertyListener<T> listener) {
            Subscription subscription = oldSubscriptions.remove(listener);
            if (subscription != null) {
                subscription.unsubscribe();
            }
        }

        @Override
        public Property<T> orElse(T defaultValue) {
            return new PropertyImpl<>(keyAndType, () -> {
                T value = supplier.get();
                return value != null ? value : defaultValue;
            });
        }

        @Override
        public Property<T> orElseGet(String key) {
            if (!keyAndType.hasType()) {
                throw new IllegalStateException("Type information lost due to map() operation.  All calls to orElse[Get] must be made prior to calling map");
            }
            KeyAndType<T> keyAndType = this.keyAndType.withKey(key);
            Property<T> next = DefaultPropertyFactory.this.get(key, keyAndType.type);
            return new PropertyImpl<>(keyAndType, () -> {
                T value = supplier.get();
                return value != null ? value : next.get();
            });
        }

        @Override
        public <S> Property<S> map(Function<T, S> mapper) {
            return new PropertyImpl<>(keyAndType.discardType(), () -> {
                T value = supplier.get();
                if (value != null) {
                    return mapper.apply(value);
                } else {
                    return null;
                }
            });
        }

        @Override
        public String toString() {
            return "Property [Key=" + getKey() + "; value="+get() + "]";
        }
    }

    private static final class KeyAndType<T> {
        private final String key;
        private final Type type;

        public KeyAndType(String key, Type type) {
            this.key = key;
            this.type = type;
        }

        public <S> KeyAndType<S> discardType() {
            if (type == null) {
                @SuppressWarnings("unchecked") // safe since type is null
                KeyAndType<S> keyAndType = (KeyAndType<S>) this;
                return keyAndType;
            }
            return new KeyAndType<>(key, null);
        }

        public KeyAndType<T> withKey(String newKey) {
            return new KeyAndType<>(newKey, type);
        }

        public boolean hasType() {
            return type != null;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + Objects.hashCode(key);
            result = 31 * result + Objects.hashCode(type);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyAndType)) {
                return false;
            }
            KeyAndType<?> other = (KeyAndType<?>) obj;
            return Objects.equals(key, other.key) && Objects.equals(type, other.type);
        }

        @Override
        public String toString() {
            return "KeyAndType{" +
                    "key='" + key + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
