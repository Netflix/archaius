package netflix.archaius.config;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import netflix.archaius.Config;
import netflix.archaius.StrInterpolator;
import netflix.archaius.interpolate.CommonsStrInterpolatorFactory;

public abstract class AbstractConfig implements Config {

    private final String name;
    private StrInterpolator interpolator;
    private Config parent;
    
    public AbstractConfig(String name) {
        this.name = name;
        this.interpolator = CommonsStrInterpolatorFactory.INSTANCE.create(this);
    }
    
    public void setStrInterpolator(StrInterpolator interpolator) {
        this.interpolator = interpolator;
    }
    
    public StrInterpolator getStrInterpolator() {
        return this.interpolator;
    }
    
    public Config getParent() {
        return parent;
    }
    
    public void setParent(Config config) {
        this.parent = config;
    }

    @Override
    public Object interpolate(String key) {
        Object prop = getRawProperty(key);
        if (prop == null) {
            return null;
        }
        return interpolator.resolve(prop.toString());
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
        Object value = getRawProperty(key);
        if (value == null) 
            return null;
        return value.toString();
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
        return get(type, null);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        Constructor<T> c;
        try {
            c = type.getConstructor(String.class);
            return c.newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
        }
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
    public Config subset(String prefix) {
        return new PrefixedViewConfig(prefix, this);
    }

    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public void accept(Visitor visitor) {
        Iterator<String> iter = getKeys();
        while (iter.hasNext()) {
            visitor.visit(this, iter.next());
        }
    }
}
