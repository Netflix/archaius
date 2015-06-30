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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.Decoder;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.StrInterpolator.Lookup;
import com.netflix.archaius.exceptions.ParseException;
import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.interpolate.ConfigStrLookup;

public abstract class AbstractConfig implements Config {

    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();
    private final Lookup lookup;
    private Decoder decoder;
    private StrInterpolator interpolator;

    public AbstractConfig() {
        this.decoder = new DefaultDecoder();
        this.interpolator = CommonsStrInterpolator.INSTANCE;
        this.lookup = ConfigStrLookup.from(this);
    }

    protected CopyOnWriteArrayList<ConfigListener> getListeners() {
        return listeners;
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
            return notFound(key, defaultValue != null ? interpolator.create(lookup).resolve(defaultValue) : null);
        }

        if (value instanceof String) {
            return interpolator.create(lookup).resolve(value.toString());
        } else {
            throw new UnsupportedOperationException(
                    "Property values other than String not supported");
        }
    }

    @Override
    public String getString(String key) {
        Object value = getRawProperty(key);
        if (value == null) {
            return notFound(key);
        }

        if (value instanceof String) {
            return interpolator.create(lookup).resolve(value.toString());
        } else {
            throw new UnsupportedOperationException(
                    "Property values other than String not supported");
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
    public Iterator<String> getKeys(String prefix) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }

        return result.iterator();
    }

    @Override
    public Config getPrefixedView(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.equals(".")) {
            return this;
        }
        return new PrefixedViewConfig(prefix, this);
    }

    @Override
    public void accept(Visitor visitor) {
        Iterator<String> iter = getKeys();
        while (iter.hasNext()) {
            visitor.visit(this, iter.next());
        }
    }

    @Override
    public Long getLong(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(Long.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return defaultValue;
        try {
            return decoder.decode(Long.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Double getDouble(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(Double.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {   
            return decoder.decode(Double.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Integer getInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);

        try {   
            return decoder.decode(Integer.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        
        try {
            return decoder.decode(Integer.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Boolean getBoolean(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        } 
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        return parseError(key, value, new Exception("Expected one of [true, yes, on, false, no, off]"));
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        } 
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        return parseError(key, value, new Exception("Expected one of [true, yes, on, false, no, off]"));
    }

    @Override
    public Short getShort(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        
        try {
            return decoder.decode(Short.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return decoder.decode(Short.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigInteger getBigInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(BigInteger.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return decoder.decode(BigInteger.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(BigDecimal.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return decoder.decode(BigDecimal.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Float getFloat(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(Float.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return decoder.decode(Float.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Byte getByte(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(key);
        try {
            return decoder.decode(Byte.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        String value = getString(key, null);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return decoder.decode(Byte.class, value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public List getList(String key) {
        String value = getString(key);
        if (value == null) {
            return notFound(key);
        }
        String[] parts = value.split(",");
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
        String value = getString(key);
        if (value == null) {
            return notFound(key);
        }
        return getDecoder().decode(type, value);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return notFound(key, defaultValue);
        }
        return getDecoder().decode(type, value);
    }

    private <T> T parseError(String key, String value, Exception e) {
        throw new ParseException("Error parsing value '" + value + "' for property '" + key + "'", e);
    }
}
