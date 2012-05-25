/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
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
 * This class delegates to read/write to an existing configuration but is also attached with 
 * a dynamic configuration source and polling scheduler so that its properties can be changed dynamically
 * at runtime.
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
    
    @Override
    public void addProperty(String key, Object value) {
        config.addProperty(key, value);
    }

    @Override
    public void clear() {
        config.clear();
    }

    @Override
    public void clearProperty(String key) {
        config.clearProperty(key);
    }


    @Override
    public boolean containsKey(String arg0) {
        return config.containsKey(arg0);
    }


    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return config.getBigDecimal(key, defaultValue);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return config.getBigDecimal(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return config.getBigInteger(key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return config.getBigInteger(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    @Override
    public byte getByte(String key, byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    @Override
    public byte getByte(String key) {
        return config.getByte(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    @Override
    public double getDouble(String key) {
        return config.getDouble(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    @Override
    public float getFloat(String key) {
        return config.getFloat(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return config.getInt(key, defaultValue);
    }

    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return config.getInteger(key, defaultValue);
    }


    @Override
    public Iterator getKeys() {
        return config.getKeys();
    }

    @Override
    public Iterator getKeys(String prefix) {
        return config.getKeys(prefix);
    }

    @Override
    public List getList(String key, List defaultValue) {
        return config.getList(key, defaultValue);
    }

    @Override
    public List getList(String key) {
        return config.getList(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    @Override
    public long getLong(String key) {
        return config.getLong(key);
    }


    @Override
    public Properties getProperties(String key) {
        return config.getProperties(key);
    }

    @Override
    public Object getProperty(String arg0) {
        return config.getProperty(arg0);
    }

    @Override
    public short getShort(String key, short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    @Override
    public short getShort(String key) {
        return config.getShort(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public String[] getStringArray(String key) {
        return config.getStringArray(key);
    }


    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }


    @Override
    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }


    @Override
    public Configuration subset(String prefix) {
        return config.subset(prefix);
    }
    
}
