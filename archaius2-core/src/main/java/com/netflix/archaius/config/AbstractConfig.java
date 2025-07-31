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
package com.netflix.archaius.config;

import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.api.ArchaiusType;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.StrInterpolator.Lookup;
import com.netflix.archaius.exceptions.ParseException;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.interpolate.ConfigStrLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public abstract class AbstractConfig implements Config {

    private static final Logger log = LoggerFactory.getLogger(AbstractConfig.class);
    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<>();
    private final Lookup lookup;
    private Decoder decoder;
    private StrInterpolator interpolator;
    private String listDelimiter = ",";
    private final String name;
    
    private static final AtomicInteger idCounter = new AtomicInteger();
    protected static String generateUniqueName(String prefix) {
        return prefix + idCounter.incrementAndGet();
    }
    
    public AbstractConfig(String name) {
        this.decoder = DefaultDecoder.INSTANCE;
        this.interpolator = CommonsStrInterpolator.INSTANCE;
        this.lookup = ConfigStrLookup.from(this);
        this.name = name == null ? generateUniqueName("unnamed-") : name;
    }
    
    public AbstractConfig() {
        this(generateUniqueName("unnamed-"));
    }

    @SuppressWarnings("unused")
    protected CopyOnWriteArrayList<ConfigListener> getListeners() {
        return listeners;
    }
    
    protected Lookup getLookup() { 
        return lookup; 
    }
    
    public String getListDelimiter() {
        return listDelimiter;
    }
    
    @SuppressWarnings("unused")
    public void setListDelimiter(String delimiter) {
        listDelimiter = delimiter;
    }

    @Override
    final public Decoder getDecoder() {
        return this.decoder;
    }

    @Override
    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    final public StrInterpolator getStrInterpolator() {
        return this.interpolator;
    }

    @Override
    public void setStrInterpolator(StrInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public void addListener(ConfigListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners that the child configuration has been updated.
     * @implNote This implementation calls listeners in the order they were added. This is NOT part of the API contract.
     * @implSpec Implementors must make sure that if a listener fails it will not prevent other listeners from being
     *   called. In practice this means that exceptions must be caught (and probably logged), but not rethrown.
     */
    protected void notifyConfigUpdated(Config child) {
        for (ConfigListener listener : listeners) {
            callSafely(() -> listener.onConfigUpdated(child));
        }
    }

    /**
     * Notify all listeners that the child configuration encountered an error
     * @implNote This implementation calls listeners in the order they were added. This is NOT part of the API contract.
     * @implSpec Implementors must make sure that if a listener fails it will not prevent other listeners from being
     *   called. In practice this means that exceptions must be caught (and probably logged), but not rethrown.
     */
    protected void notifyError(Throwable t, Config child) {
        for (ConfigListener listener : listeners) {
            callSafely(() -> listener.onError(t, child));
        }
    }

    /**
     * Notify all listeners that a child configuration has been added.
     * @implNote This implementation calls listeners in the order they were added. This is NOT part of the API contract.
     * @implSpec Implementors must make sure that if a listener fails it will not prevent other listeners from being
     *   called. In practice this means that exceptions must be caught (and probably logged), but not rethrown.
     */
    protected void notifyConfigAdded(Config child) {
        for (ConfigListener listener : listeners) {
            callSafely(() -> listener.onConfigAdded(child));
        }
    }

    /**
     * Notify all listeners that the child configuration has been removed.
     * @implNote This implementation calls listeners in the order they were added. This is NOT part of the API contract.
     * @implSpec Implementors must make sure that if a listener fails it will not prevent other listeners from being
     *   called. In practice this means that exceptions must be caught (and probably logged), but not rethrown.
     */
    protected void notifyConfigRemoved(Config child) {
        for (ConfigListener listener : listeners) {
            callSafely(() -> listener.onConfigRemoved(child));
        }
    }

    private void callSafely(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException t) {
            log.error("A listener on config {} failed when receiving a notification. listener={}", this, r, t);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        Object value = getRawProperty(key);
        if (value == null) {
            return notFound(key, defaultValue != null ? interpolator.create(getLookup()).resolve(defaultValue) : null);
        }

        if (value instanceof String) {
            return resolve((String)value);
        } else {
            return value.toString();
        }
    }

    @Override
    public String getString(String key) {
        Object value = getRawProperty(key);
        if (value == null) {
            return notFound(key);
        }

        if (value instanceof String) {
            // todo: inconsistent with above
            return resolve(value.toString());
        } else {
            return value.toString();
        }
    }

    /**
     * Handle notFound when a defaultValue is provided.
     * This implementation simply returns the defaultValue. Subclasses can override this method to provide
     * alternative behavior.
     */
    protected <T> T notFound(@SuppressWarnings("unused") String key, T defaultValue) {
        return defaultValue;
    }

    /**
     * Handle notFound when no defaultValue is provided.
     * This implementation throws a NoSuchElementException. Subclasses can override this method to provide
     * alternative behavior.
     */
    protected <T> T notFound(String key) {
        throw new NoSuchElementException("'" + key + "' not found");
    }

    @Override
    @Deprecated
    public Iterator<String> getKeys(final String prefix) {
        return new Iterator<String>() {
            final Iterator<String> iter = getKeys();
            String next;

            {
                while (iter.hasNext()) {
                    next = iter.next();
                    if (next.startsWith(prefix)) {
                        break;
                    }
                    else {
                        next = null;
                    }
                }
            }
            
            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public String next() {
                if (next == null) {
                    throw new IllegalStateException();
                }
                
                String current = next;
                next = null;
                while (iter.hasNext()) {
                    next = iter.next();
                    if (next.startsWith(prefix)) {
                        break;
                    }
                    else {
                        next = null;
                    }
                }
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Config getPrefixedView(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.equals(".")) {
            return this;
        }
        return new PrefixedViewConfig(prefix, this);
    }

    @Override
    public Config getPrivateView() {
        return new PrivateViewConfig(this);
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        // The general visitor pattern is not expected to have consequences at the individual key-value pair level,
        // so we choose to leave visitors uninstrumented as they otherwise represent a large source of noisy data.
        forEachPropertyUninstrumented(visitor::visitKey);
        return null;
    }

    protected <T> T getValue(Type type, String key) {
        T value = getValueWithDefault(type, key, null);
        if (value == null) {
            return notFound(key);
        } else {
            return value;
        }
    }

    private boolean isMap(ParameterizedType type) {
        if (type.getRawType() instanceof Class) {
            return Map.class.isAssignableFrom((Class<?>) type.getRawType());
        }
        return false;
    }

    private boolean isCollection(ParameterizedType type) {
        if (type.getRawType() instanceof Class) {
            return Collection.class.isAssignableFrom((Class<?>) type.getRawType());
        }
        return false;

    }

    @SuppressWarnings("unchecked")
    protected <T> T getValueWithDefault(Type type, String key, T defaultValue) {
        Object rawProp = getRawProperty(key);
        if (rawProp == null && type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (isMap(parameterizedType)) {
                Map<?, ?> ret = new LinkedHashMap<>();
                String keyAndDelimiter = key + ".";
                Type keyType = parameterizedType.getActualTypeArguments()[0];
                Type valueType = parameterizedType.getActualTypeArguments()[1];

                for (String k : keys()) {
                    if (k.startsWith(keyAndDelimiter)) {
                        ret.put(getDecoder().decode(keyType, k.substring(keyAndDelimiter.length())), get(valueType, k));
                    }
                }
                return ret.isEmpty() ? defaultValue : (T) Collections.unmodifiableMap(ret);
            } else if (isCollection(parameterizedType)) {
                Type valueType = parameterizedType.getActualTypeArguments()[0];
                List<?> ret = createListForKey(key, valueType);
                if (Set.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                    Set<?> retSet = new LinkedHashSet<>(ret);
                    return ret.isEmpty() ? defaultValue : (T) Collections.unmodifiableSet(retSet);
                } else {
                    return ret.isEmpty() ? defaultValue : (T) Collections.unmodifiableList(ret);
                }
            }
        }

        // Not found. Return the default.
        if (rawProp == null) {
            return defaultValue;
        }

        // raw prop is a String. Decode it or fail.
        if (rawProp instanceof String) {
            try {
                String value = resolve(rawProp.toString());
                return decoder.decode(type, value);
            } catch (RuntimeException e) {
                return parseError(key, rawProp.toString(), e);
            }
        }

        if (type instanceof Class) {
            // Caller wants a simple class.
            Class<?> cls = (Class<?>) type;

            // The raw object is already of the right type
            if (cls.isInstance(rawProp)) {
                return (T) rawProp;
            }

            // Caller wants a string
            if (String.class.isAssignableFrom(cls)) {
                return (T) rawProp.toString();
            }

            // Caller wants an unwrapped boolean.
            if (rawProp instanceof Boolean &&  cls == boolean.class) {
                return (T) rawProp;
            }

            // Caller wants a number AND we have one. Handle widening and narrowing conversions.
            // Char is not included here. It's not a Number and the semantics of converting it to/from a number or a
            // string have rough edges. Just ask users to avoid it.
            if (rawProp instanceof Number
                && ( Number.class.isAssignableFrom(cls)
                   || ( cls.isPrimitive() && cls != char.class ))) { // We handled boolean above, so if cls is a primitive and not char then it's a number type
                if (cls == int.class || cls == Integer.class) {
                    return (T) Integer.valueOf(((Number) rawProp).intValue());
                }
                if (cls == long.class || cls == Long.class) {
                    return (T) Long.valueOf(((Number) rawProp).longValue());
                }
                if (cls == double.class || cls == Double.class) {
                    return (T) Double.valueOf(((Number) rawProp).doubleValue());
                }
                if (cls == float.class || cls == Float.class) {
                    return (T) Float.valueOf(((Number) rawProp).floatValue());
                }
                if (cls == short.class || cls == Short.class) {
                    return (T) Short.valueOf(((Number) rawProp).shortValue());
                }
                if (cls == byte.class || cls == Byte.class) {
                    return (T) Byte.valueOf(((Number) rawProp).byteValue());
                }
            }
        }

        // Nothing matches (ie, caller wants a ParametrizedType, or the rawProp is not easily cast to the desired type)
        return parseError(key, rawProp.toString(),
                new IllegalArgumentException("Property " + rawProp + " is not convertible to " + type.getTypeName()));
    }

    private List<?> createListForKey(String key, Type type) {
        List<?> vals = new ArrayList<>();
        int counter = 0;
        while (true) {
            String checkKey = String.format("%s[%s]", key, counter++);
            if (containsKey(checkKey)) {
                vals.add(get(type, checkKey));
            } else {
                break;
            }
        }
        return vals;
    }

    @Override
    public String resolve(String value) {
        return interpolator.create(getLookup()).resolve(value);
    }

    @Override
    public <T> T resolve(String value, Class<T> type) {
        return getDecoder().decode((Type) type, resolve(value));
    }

    @Override
    public Long getLong(String key) {
        return getValue(Long.class, key);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return getValueWithDefault(Long.class, key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return getValue(Double.class, key);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return getValueWithDefault(Double.class, key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return getValue(Integer.class, key);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return getValueWithDefault(Integer.class, key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key) {
        return getValue(Boolean.class, key);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return getValueWithDefault(Boolean.class, key, defaultValue);
    }

    @Override
    public Short getShort(String key) {
        return getValue(Short.class, key);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return getValueWithDefault(Short.class, key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return getValue(BigInteger.class, key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return getValueWithDefault(BigInteger.class, key, defaultValue);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return getValue(BigDecimal.class, key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return getValueWithDefault(BigDecimal.class, key, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return getValue(Float.class, key);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return getValueWithDefault(Float.class, key, defaultValue);
    }

    @Override
    public Byte getByte(String key) {
        return getValue(Byte.class, key);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return getValueWithDefault(Byte.class, key, defaultValue);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> type) {
        Object value = getRawProperty(key);
        if (value == null) {
            List<?> alternativeListCreation = createListForKey(key, type);
            if (!alternativeListCreation.isEmpty()) {
                return (List<T>) alternativeListCreation;
            }
        }
        if (value == null) {
            return notFound(key);
        }

        // TODO: handle the case where value is a collection
        return decoder.decode(ArchaiusType.forListOf(type), resolve(value.toString()));
    }

    /**
     * @inheritDoc
     * This implementation always parses the underlying property as a comma-delimited string and returns
     * a {@code List<String>}.
     */
    @Override
    public List<?> getList(String key) {
        return getList(key, String.class);
    }

    @Override
    @SuppressWarnings("rawtypes") // Required by legacy API
    public List getList(String key, List defaultValue) {
        Object value = getRawProperty(key);
        if (value == null) {
            List<?> alternativeListCreation = createListForKey(key, String.class);
            if (!alternativeListCreation.isEmpty()) {
                return alternativeListCreation;
            }
        }
        if (value == null) {
            return notFound(key, defaultValue);
        }

        // TODO: handle the case where value is a collection
        return decoder.decode(ArchaiusType.forListOf(String.class), resolve(value.toString()));
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return getValue(type, key);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        return getValueWithDefault(type, key, defaultValue);
    }

    @Override
    public <T> T get(Type type, String key) {
        return getValue(type, key);
    }

    @Override
    public <T> T get(Type type, String key, T defaultValue) {
        return getValueWithDefault(type, key, defaultValue);
    }


    private <T> T parseError(String key, String value, Exception e) {
        throw new ParseException("Error parsing value '" + value + "' for property '" + key + "'", e);
    }
    
    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        for (String key : keys()) {
            Object value = this.getRawProperty(key);
            if (value != null) {
                consumer.accept(key, value);
            }
        }
    }

    @Override
    public String getName() { 
        return name; 
    }
}
