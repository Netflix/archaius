/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

/**
 * This class delegates property read/write to an another configuration but is also attached with 
 * a dynamic configuration source and polling scheduler so that its properties can be changed dynamically
 * at runtime. In other words, if the same property is defined in both the original configuration 
 * and the dynamic configuration source, the value in the original configuration will be overridden.
 * <p>
 * This class can be served as a decorator to an existing configuration to make the property values 
 * dynamic.
 * 
 * @author awang
 *
 */
public class ConfigurationWithPollingSource implements Configuration {

    private final Configuration config;
    private final AbstractPollingScheduler scheduler;
    
    /**
     * Create an instance and start polling the configuration source
     * 
     * @param config Configuration to delegate to
     * @param source {@link PolledConfigurationSource} to poll get new/changed properties
     * @param scheduler AbstractPollingScheduler to provide the polling schedule
     */
    public ConfigurationWithPollingSource(Configuration config, PolledConfigurationSource source, 
            AbstractPollingScheduler scheduler) {
        this.config = config;
        this.scheduler = scheduler;
        scheduler.startPolling(source, this);
    }

    public final Configuration getConfiguration() {
        return config;
    }
    
    public final void stopPolling() {
        scheduler.stop();
    }
    
    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public void addProperty(String key, Object value) {
        config.addProperty(key, value);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public void clear() {
        config.clear();
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public void clearProperty(String key) {
        config.clearProperty(key);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public boolean containsKey(String arg0) {
        return config.containsKey(arg0);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return config.getBigDecimal(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public BigDecimal getBigDecimal(String key) {
        return config.getBigDecimal(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return config.getBigInteger(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public BigInteger getBigInteger(String key) {
        return config.getBigInteger(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public byte getByte(String key, byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public byte getByte(String key) {
        return config.getByte(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public double getDouble(String key, double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Double getDouble(String key, Double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public double getDouble(String key) {
        return config.getDouble(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public float getFloat(String key, float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Float getFloat(String key, Float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public float getFloat(String key) {
        return config.getFloat(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public int getInt(String key, int defaultValue) {
        return config.getInt(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return config.getInteger(key, defaultValue);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Iterator getKeys() {
        return config.getKeys();
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Iterator getKeys(String prefix) {
        return config.getKeys(prefix);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public List getList(String key, List defaultValue) {
        return config.getList(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public List getList(String key) {
        return config.getList(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Long getLong(String key, Long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public long getLong(String key) {
        return config.getLong(key);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Properties getProperties(String key) {
        return config.getProperties(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Object getProperty(String arg0) {
        return config.getProperty(arg0);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public short getShort(String key, short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Short getShort(String key, Short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public short getShort(String key) {
        return config.getShort(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public String[] getStringArray(String key) {
        return config.getStringArray(key);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }


    /**
     * Delegates to the underlying configuration.
     */
    @Override
    public Configuration subset(String prefix) {
        return config.subset(prefix);
    }
    
}
