package com.netflix.archaius.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import com.netflix.archaius.exceptions.ParseException;

public abstract class InterpolatingConfig extends AbstractConfig {
    public InterpolatingConfig(String name) {
        super(name);
    }
    
    @Override
    public String interpolate(String key) {
        String value = getRawString(key);
        if (value == null) {
            return null;    // TODO: Should this thrown an exception?
        }
        return getStrInterpolator().resolve(value);
    }
    
    @Override
    public Long getLong(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return defaultValue;
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public String getString(String key) {
        Object value = interpolate(key);
        if (value == null) 
            return notFound(key);
        return value.toString();
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        return value;
    }

    @Override
    public Double getDouble(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {   
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Integer getInteger(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);

        try {   
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Boolean getBoolean(String key) {
        String value = interpolate(key);
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
        String value = interpolate(key);
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
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        
        try {
            return Short.parseShort(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return Short.parseShort(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigInteger getBigInteger(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return new BigInteger(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return new BigInteger(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return new BigDecimal(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return new BigDecimal(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Float getFloat(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Byte getByte(String key) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key);
        try {
            return Byte.parseByte(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        String value = interpolate(key);
        if (value == null) 
            return notFound(key, defaultValue);
        try {
            return Byte.parseByte(value);
        }
        catch (NumberFormatException e) {
            return parseError(key, value, e);
        }
    }

    @Override
    public List getList(String key) {
        String value = interpolate(key);
        if (value == null) {
            return notFound(key);
        }
        String[] parts = value.split(",");
        return Arrays.asList(parts);
    }

    @Override
    public List getList(String key, List defaultValue) {
        String value = interpolate(key);
        if (value == null) {
            return notFound(key, defaultValue);
        }
        String[] parts = value.split(",");
        return Arrays.asList(parts);
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return get(type, key, null);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        String value = interpolate(key);
        if (value == null) {
            return notFound(key, defaultValue);
        }
        return getDecoder().decode(type, value);
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

    /**
     * Handle notFound when a defaultValue is provided.
     * @param defaultValue
     * @return
     */
    private <T> T notFound(String key, T defaultValue) {
        return defaultValue;
    }
    
    private <T> T notFound(String key) {
        throw new NoSuchElementException("'" + key + "' not found in configuration " + getName());
    }
    
    private <T> T parseError(String key, String value, Exception e) {
        throw new ParseException("Error parsing value '" + value + "' for property '" + key + "'", e);
    }
}
