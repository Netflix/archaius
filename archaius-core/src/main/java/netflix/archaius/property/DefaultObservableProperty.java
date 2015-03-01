package netflix.archaius.property;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import netflix.archaius.Config;
import netflix.archaius.ObservableProperty;
import netflix.archaius.PropertyObserver;
import netflix.archaius.PropertySubscription;
import netflix.archaius.TypedPropertyObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultObservableProperty implements ObservableProperty {
    private final Logger LOG = LoggerFactory.getLogger(DefaultObservableProperty.class);
    
    private final String key;
    private final Config config;
    private final CopyOnWriteArrayList<Subscriber<?>> subscribers = new CopyOnWriteArrayList<Subscriber<?>>();
    
    public DefaultObservableProperty(String key, Config config) {
        this.key = key;
        this.config = config;
    }

    @Override
    public void update() {
        for (Subscriber<?> subscriber : subscribers) {
            subscriber.update();
        }
    }

    @Override
    public <T> PropertySubscription subscribe(PropertyObserver<T> observer, Class<T> type, final T defaultValue) {
        Subscriber<?> subscriber = null;
        
        if (type.equals(Integer.class)) {
            subscriber = new Subscriber<Integer>(observer) {
                @Override
                protected Integer getCurrent() throws Exception {
                    return config.getInteger(key, (Integer) defaultValue);
                }
            };
        }
        else if (type.equals(String.class)) {
            subscriber = new Subscriber<String>(observer) {
                @Override
                protected String getCurrent() throws Exception {
                    return config.getString(key, (String)defaultValue);
                }
            };
        }
        else if (type.equals(Boolean.class)) {
            subscriber = new Subscriber<Boolean>(observer) {
                @Override
                protected Boolean getCurrent() throws Exception {
                    return config.getBoolean(key, (Boolean)defaultValue);
                }
            };
        }
        else if (type.equals(Long.class)) {
            subscriber = new Subscriber<Long>(observer) {
                @Override
                protected Long getCurrent() throws Exception {
                    return config.getLong(key, (Long)defaultValue);
                }
            };
        }
        else if (type.equals(BigDecimal.class)) {
            subscriber = new Subscriber<BigDecimal>(observer) {
                @Override
                protected BigDecimal getCurrent() throws Exception {
                    return config.getBigDecimal(key, (BigDecimal)defaultValue);
                }
            };
        }
        else if (type.equals(Double.class)) {
            subscriber = new Subscriber<Double>(observer) {
                @Override
                protected Double getCurrent() throws Exception {
                    return config.getDouble(key, (Double)defaultValue);
                }
            };
        }
        else if (type.equals(BigInteger.class)) {
            subscriber = new Subscriber<BigInteger>(observer) {
                @Override
                protected BigInteger getCurrent() throws Exception {
                    return config.getBigInteger(key, (BigInteger)defaultValue);
                }
            };
        }
        else if (type.equals(Byte.class)) {
            subscriber = new Subscriber<Byte>(observer) {
                @Override
                protected Byte getCurrent() throws Exception {
                    return config.getByte(key, (Byte)defaultValue);
                }
            };
        }
        else if (type.equals(Float.class)) {
            subscriber = new Subscriber<Float>(observer) {
                @Override
                protected Float getCurrent() throws Exception {
                    return config.getFloat(key, (Float)defaultValue);
                }
            };
        }
        else if (type.equals(List.class)) {
            subscriber = new Subscriber<List>(observer) {
                @Override
                protected List getCurrent() throws Exception {
                    return config.getList(key, (List)defaultValue);
                }
            };
        }
        else if (type.equals(Short.class)) {
            subscriber = new Subscriber<Short>(observer) {
                @Override
                protected Short getCurrent() throws Exception {
                    return config.getShort(key, (Short)defaultValue);
                }
            };
        }
        else {
            final Constructor<T> constructor;
            try {
                constructor = type.getConstructor(String.class);
                if (constructor != null) {
                    subscriber = new Subscriber<T>(observer) {
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
                    };
                }
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            }
            
            throw new UnsupportedOperationException("Not parser for type " + type.getName());
        }
        
        subscribers.add(subscriber);
        subscriber.update();
        return subscriber;
    }
    
    public abstract class Subscriber<T> implements PropertySubscription {
        private T existing = null;
        private PropertyObserver<?> observer;
        
        public Subscriber(PropertyObserver<?> observer) {
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
                    ((PropertyObserver<T>)observer).onChange(key, existing, next);
                    existing = next;
                }
            }
            catch (Exception e) {
                LOG.warn("Unable to get current version of property '{}'. Error: {}", key, e.getMessage());
            }
        }
        
        protected abstract T getCurrent() throws Exception;
    }

    @Override
    public <T> PropertySubscription subscribe(TypedPropertyObserver<T> observer) {
        return subscribe(observer, observer.getType(), observer.getDefaultValue());
    }
}
