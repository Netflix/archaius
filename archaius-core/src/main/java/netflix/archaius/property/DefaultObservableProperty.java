package netflix.archaius.property;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;

import netflix.archaius.Config;
import netflix.archaius.ObservableProperty;
import netflix.archaius.Property;
import netflix.archaius.PropertyObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultObservableProperty implements ObservableProperty {
    private final Logger LOG = LoggerFactory.getLogger(DefaultObservableProperty.class);
    
    private final String key;
    private final Config config;
    private final CopyOnWriteArrayList<AbstractProperty<?>> subscribers = new CopyOnWriteArrayList<AbstractProperty<?>>();
    
    public DefaultObservableProperty(String key, Config config) {
        this.key = key;
        this.config = config;
    }

    @Override
    public void update() {
        for (AbstractProperty<?> subscriber : subscribers) {
            subscriber.update();
        }
    }

    public abstract class AbstractProperty<T> implements Property<T> {
        private volatile T existing = null;
        private PropertyObserver<?> observer;
        
        public AbstractProperty(PropertyObserver<?> observer) {
            this.observer = observer;
        }
        
        @Override
        public void unsubscribe() {
            subscribers.remove(this);
        }

        void update() {
            try {
                T next = getCurrent();
                if ((next == null || existing == null) && next == existing) {
                    return;
                }
                else if (next == null || existing == null || !existing.equals(next)) {
                    
                    
                    existing = next;
                    if (observer != null) {
                        ((PropertyObserver<T>)observer).onChange(existing);
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Unable to get current version of property '{}'. Error: {}", key, e.getMessage());
                if (observer != null) {
                    ((PropertyObserver<T>)observer).onError(e);
                }
            }
        }
        
        @Override
        public T get() {
            return existing;
        }

        protected abstract T getCurrent() throws Exception;
    }

    private <T> AbstractProperty<T> subscribe(AbstractProperty<T> subscriber) {
        subscribers.add(subscriber);
        subscriber.update();
        return subscriber;
    }
    
    @Override
    public Property<String> asString(final String defaultValue, PropertyObserver<String> observer) {
        return subscribe(new AbstractProperty<String>(observer) {
            @Override
            protected String getCurrent() throws Exception {
                return config.getString(key, defaultValue);
            }
        });
    }

    @Override
    public Property<Integer> asInteger(final Integer defaultValue, PropertyObserver<Integer> observer) {
        return subscribe(new AbstractProperty<Integer>(observer) {
            @Override
            protected Integer getCurrent() throws Exception {
                return config.getInteger(key, defaultValue);
            }
        });
    }

    @Override
    public Property<Double> asDouble(final Double defaultValue, PropertyObserver<Double> observer) {
        return subscribe(new AbstractProperty<Double>(observer) {
            @Override
            protected Double getCurrent() throws Exception {
                return config.getDouble(key, defaultValue);
            }
        });
    }

    @Override
    public Property<Float> asFloat(final Float defaultValue, PropertyObserver<Float> observer) {
        return subscribe(new AbstractProperty<Float>(observer) {
            @Override
            protected Float getCurrent() throws Exception {
                return config.getFloat(key, defaultValue);
            }
        });
    }

    @Override
    public Property<Short> asShort(final Short defaultValue, PropertyObserver<Short> observer) {
        return subscribe(new AbstractProperty<Short>(observer) {
            @Override
            protected Short getCurrent() throws Exception {
                return config.getShort(key, defaultValue);
            }
        });
    }

    @Override
    public Property<Byte> asByte(final Byte defaultValue, PropertyObserver<Byte> observer) {
        return subscribe(new AbstractProperty<Byte>(observer) {
            @Override
            protected Byte getCurrent() throws Exception {
                return config.getByte(key, defaultValue);
            }
        });
    }

    @Override
    public Property<BigDecimal> asBigDecimal(final BigDecimal defaultValue, PropertyObserver<BigDecimal> observer) {
        return subscribe(new AbstractProperty<BigDecimal>(observer) {
            @Override
            protected BigDecimal getCurrent() throws Exception {
                return config.getBigDecimal(key, defaultValue);
            }
        });
    }
    
    @Override
    public Property<Boolean> asBoolean(final Boolean defaultValue, PropertyObserver<Boolean> observer) {
        return subscribe(new AbstractProperty<Boolean>(observer) {
            @Override
            protected Boolean getCurrent() throws Exception {
                return config.getBoolean(key, defaultValue);
            }
        });
    }

    @Override
    public Property<BigInteger> asBigInteger(final BigInteger defaultValue, PropertyObserver<BigInteger> observer) {
        return subscribe(new AbstractProperty<BigInteger>(observer) {
            @Override
            protected BigInteger getCurrent() throws Exception {
                return config.getBigInteger(key, defaultValue);
            }
        });
    }

    @Override
    public <T> Property<T> asType(Class<T> type, final T defaultValue, PropertyObserver<T> observer) {
        final Constructor<T> constructor;
        try {
            constructor = type.getConstructor(String.class);
            if (constructor != null) {
                return subscribe(new AbstractProperty<T>(observer) {
                    @Override
                    protected T getCurrent() throws Exception {
                        String value = config.getString(key);
                        if (value == null) {
                            return defaultValue;
                        }
                        else { 
                            return constructor.newInstance(value);
                        }
                    }
                });
            }
        } catch (NoSuchMethodException e) {
        } catch (SecurityException e) {
            throw new UnsupportedOperationException("No parser for type " + type.getName());
        }
      
        throw new UnsupportedOperationException("No parser for type " + type.getName());
    }

    @Override
    public Property<String> asString(String defaultValue) {
        return asString(defaultValue, null);
    }

    @Override
    public Property<Integer> asInteger(Integer defaultValue) {
        return asInteger(defaultValue, null);
    }

    @Override
    public Property<Double> asDouble(Double defaultValue) {
        return asDouble(defaultValue, null);
    }

    @Override
    public Property<Float> asFloat(Float defaultValue) {
        return asFloat(defaultValue, null);
    }

    @Override
    public Property<Short> asShort(Short defaultValue) {
        return asShort(defaultValue, null);
    }

    @Override
    public Property<Byte> asByte(Byte defaultValue) {
        return asByte(defaultValue, null);
    }

    @Override
    public Property<BigDecimal> asBigDecimal(BigDecimal defaultValue) {
        return asBigDecimal(defaultValue, null);
    }

    @Override
    public Property<Boolean> asBoolean(Boolean defaultValue) {
        return asBoolean(defaultValue, null);
    }

    @Override
    public Property<BigInteger> asBigInteger(BigInteger defaultValue) {
        return asBigInteger(defaultValue, null);
    }

    @Override
    public <T> Property<T> asType(Class<T> type, T defaultValue) {
        return asType(type, defaultValue, null);
    }
}
