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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import com.netflix.archaius.Config;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.interpolate.CommonsStrInterpolatorFactory;

public abstract class DelegatingConfig implements Config {
    
    private final String name;
    private Config parent;
    private StrInterpolator interpolator;

    public DelegatingConfig(String name) {
        this.name = name;
        this.interpolator = CommonsStrInterpolatorFactory.INSTANCE.create(this);
    }
    
    protected abstract Config getConfigWithProperty(String key, boolean failOnNotFound);
    
    @Override
    public String getRawString(String key) {
        return getConfigWithProperty(key, false).getRawString(key);
    }
    
    @Override
    public Long getLong(String key) {
        return getConfigWithProperty(key, true).getLong(key);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getLong(key);
    }

    @Override
    public String getString(String key) {
        return getConfigWithProperty(key, true).getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getString(key);
    }

    @Override
    public Double getDouble(String key) {
        return getConfigWithProperty(key, true).getDouble(key);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getDouble(key);
    }

    @Override
    public Integer getInteger(String key) {
        return getConfigWithProperty(key, true).getInteger(key);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getInteger(key);
    }

    @Override
    public Boolean getBoolean(String key) {
        return getConfigWithProperty(key, true).getBoolean(key);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getBoolean(key);
    }

    @Override
    public Short getShort(String key) {
        return getConfigWithProperty(key, true).getShort(key);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getShort(key);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return getConfigWithProperty(key, true).getBigInteger(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getBigInteger(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return getConfigWithProperty(key, true).getBigDecimal(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getBigDecimal(key);
    }

    @Override
    public Float getFloat(String key) {
        return getConfigWithProperty(key, true).getFloat(key);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getFloat(key);
    }

    @Override
    public Byte getByte(String key) {
        return getConfigWithProperty(key, true).getByte(key);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getByte(key);
    }

    @Override
    public List<?> getList(String key) {
        return getConfigWithProperty(key, true).getList(key);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.getList(key);
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return getConfigWithProperty(key, true).get(type, key);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        Config config = getConfigWithProperty(key, false);
        if (config == null)
            return defaultValue;
        return config.get(type, key);
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
    public String getName() {
        return name;
    }

    @Override
    public String interpolate(String key) {
        return interpolator.resolve(key);
    }

    @Override
    public Config subset(String prefix) {
        return new PrefixedViewConfig(prefix, this);
    }

    @Override
    public Config getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Config config) {
        this.parent = config;
    }

    public void setStrInterpolator(StrInterpolator interpolator) {
        this.interpolator = interpolator;
    }
    
    public StrInterpolator getStrInterpolator() {
        return this.interpolator;
    }
}
