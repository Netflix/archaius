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
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.StrInterpolator.Lookup;
import com.netflix.archaius.exceptions.ParseException;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.interpolate.ConfigStrLookup;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public abstract class AbstractConfig implements Config {

    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();
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
        this.decoder = new DefaultDecoder();
        this.interpolator = CommonsStrInterpolator.INSTANCE;
        this.lookup = ConfigStrLookup.from(this);
        this.name = name == null ? generateUniqueName("unnamed-") : name;
    }
    
    public AbstractConfig() {
        this(generateUniqueName("unnamed-"));
    }

    protected CopyOnWriteArrayList<ConfigListener> getListeners() {
        return listeners;
    }
    
    protected Lookup getLookup() { 
        return lookup; 
    }
    
    public String getListDelimiter() {
        return listDelimiter;
    }
    
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

    protected void notifyConfigUpdated(Config child) {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated(child);
        }
    }

    protected void notifyError(Throwable t, Config child) {
        for (ConfigListener listener : listeners) {
            listener.onError(t, child);
        }
    }

    protected void notifyConfigAdded(Config child) {
        for (ConfigListener listener : listeners) {
            listener.onConfigAdded(child);
        }
    }

    protected void notifyConfigRemoved(Config child) {
        for (ConfigListener listener : listeners) {
            listener.onConfigRemoved(child);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        Object value = getRawProperty(key);
        if (value == null) {
            return notFound(key, defaultValue != null ? interpolator.create(getLookup()).resolve(defaultValue) : null);
        }

        if (value instanceof String) {
            return interpolator.create(getLookup()).resolve(value.toString());
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
            return interpolator.create(getLookup()).resolve(value.toString());
        } else {
            return value.toString();
        }
    }

    /**
     * Handle notFound when a defaultValue is provided.
     * @param defaultValue
     * @return
     */
    protected <T> T notFound(String key, T defaultValue) {
        return defaultValue;
    }
    
    protected <T> T notFound(String key) {
        throw new NoSuchElementException("'" + key + "' not found");
    }

    @Override
    @Deprecated
    public Iterator<String> getKeys(final String prefix) {
        return new Iterator<String>() {
            Iterator<String> iter = getKeys();
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
    public <T> T accept(Visitor<T> visitor) {
        T result = null;
        forEachProperty((k, v) -> visitor.visitKey(k, v));
        return null;
    }

    protected <T> T getValue(Class<T> type, String key) {
        T value = getValueWithDefault(type, key, null);
        if (value == null) {
            return notFound(key);
        } else {
            return value;
        }
    }

    protected <T> T getValueWithDefault(Class<T> type, String key, T defaultValue) {
        Object rawProp = getRawProperty(key);
        if (rawProp == null) {
            return defaultValue;
        }
        if (rawProp instanceof String) {
            try {
                String value = interpolator.create(getLookup()).resolve(rawProp.toString());
                return decoder.decode(type, value);
            } catch (NumberFormatException e) {
                return parseError(key, rawProp.toString(), e);
            }
        } else if (type.isInstance(rawProp)) {
            return (T)rawProp;
        } else {
            return parseError(key, rawProp.toString(),
                    new NumberFormatException("Property " + rawProp.toString() + " is of wrong format " + type.getCanonicalName()));
        }
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
        String value = getString(key);
        if (value == null) {
            return notFound(key);
        }
        String[] parts = value.split(getListDelimiter());
        List<T> result = new ArrayList<T>();
        for (String part : parts) {
            result.add(decoder.decode(type, part));
        }
        return result;
    }

    @Override
    public List getList(String key) {
        String value = getString(key);
        if (value == null) {
            return notFound(key);
        }
        String[] parts = value.split(getListDelimiter());
        return Arrays.asList(parts);
    }

    @Override
    public List getList(String key, List defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return notFound(key, defaultValue);
        }
        String[] parts = value.split(",");
        return Arrays.asList(parts);
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return getValue(type, key);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        return getValueWithDefault(type, key, defaultValue);
    }

    private <T> T parseError(String key, String value, Exception e) {
        throw new ParseException("Error parsing value '" + value + "' for property '" + key + "'", e);
    }
    
    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
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
