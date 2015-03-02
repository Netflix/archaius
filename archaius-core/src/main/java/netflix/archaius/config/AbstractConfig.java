package netflix.archaius.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import netflix.archaius.Config;
import netflix.archaius.StrInterpolator;

public abstract class AbstractConfig implements Config {

    private final String name;
    private StrInterpolator interpolator;
    
    public AbstractConfig(String name) {
        this.name = name;
    }
    
    public void setStrInterpolator(StrInterpolator interpolator) {
        this.interpolator = interpolator;
    }
    
    public StrInterpolator getStrInterpolator() {
        return this.interpolator;
    }
    
    @Override
    public String interpolate(String key) {
        return (String) interpolator.resolve(getProperty(key));
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getLong(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Long.valueOf(value);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Long.valueOf(value);
    }

    @Override
    public String getString(String key) {
        return interpolate(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return value;
    }

    @Override
    public Double getDouble(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Double.valueOf(value);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Double.valueOf(value);
    }

    @Override
    public Integer getInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Integer.valueOf(value);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Integer.valueOf(value);
    }

    @Override
    public Boolean getBoolean(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Boolean.valueOf(value);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Boolean.valueOf(value);
    }

    @Override
    public Short getShort(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Short.valueOf(value);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Short.valueOf(value);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return BigInteger.valueOf(Long.valueOf(value));
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return BigInteger.valueOf(Long.valueOf(value));
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return BigDecimal.valueOf(Long.valueOf(value));
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return BigDecimal.valueOf(Long.valueOf(value));
    }

    @Override
    public Float getFloat(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Float.valueOf(value);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Float.valueOf(value);
    }

    @Override
    public Byte getByte(String key) {
        String value = getString(key);
        if (value == null) 
            return null;
        return Byte.valueOf(value);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        return Byte.valueOf(value);
    }

    @Override
    public List getList(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List getList(String key, List defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<String> getKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Config subset(String prefix) {
        // TODO Auto-generated method stub
        return null;
    }

}
