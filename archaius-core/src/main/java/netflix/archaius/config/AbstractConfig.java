package netflix.archaius.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import netflix.archaius.Config;
import netflix.archaius.StrInterpolator;
import netflix.archaius.interpolate.CommonsStrInterpolatorFactory;

import com.google.common.collect.Lists;

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
            return notFound(null);
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public String getString(String key) {
        Object value = getRawProperty(key);
        if (value == null) 
            return notFound();
        return value.toString();
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        return value;
    }

    @Override
    public Double getDouble(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        try {   
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public Integer getInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();

        try {   
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public Boolean getBoolean(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound(null);
        
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        } 
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        return parseError(null);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key);
        if (value == null) 
            return defaultValue;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        } 
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        return parseError(null, defaultValue);
    }

    @Override
    public Short getShort(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        
        try {
            return Short.parseShort(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        try {
            return Short.parseShort(value);
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public BigInteger getBigInteger(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return BigInteger.valueOf(Long.valueOf(value));
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return BigInteger.valueOf(Long.valueOf(value));
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return BigDecimal.valueOf(Long.valueOf(value));
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        try {
            return BigDecimal.valueOf(Long.valueOf(value));
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public Float getFloat(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Byte getByte(String key) {
        String value = getString(key);
        if (value == null) 
            return notFound();
        try {
            return Byte.parseByte(value);
        }
        catch (NumberFormatException e) {
            return parseError(e);
        }
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        String value = getString(key);
        if (value == null) 
            return notFound(defaultValue);
        try {
            return Byte.parseByte(value);
        }
        catch (NumberFormatException e) {
            return parseError(e, defaultValue);
        }
    }

    @Override
    public List getList(String key) {
        String value = getString(key);
        if (value == null) {
            return notFound();
        }
        String[] parts = value.split(",");
        return Lists.newArrayList(parts);
    }

    @Override
    public List getList(String key, List defaultValue) {
        String value = getString(key);
        if (value == null) {
            return notFound(defaultValue);
        }
        String[] parts = value.split(",");
        return Lists.newArrayList(parts);
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
            if (c != null) {
                return c.newInstance(value);
            }
            
            Method method = type.getMethod("valueOf", String.class);
            if (method != null) {
                return (T) method.invoke(null, value);
            }
            throw new RuntimeException(type.getCanonicalName() + " has no String constructor or valueOf static method");
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

    private <T> T notFound(T defaultValue) {
        return defaultValue;
    }
    
    private <T> T notFound() {
        return null;
    }
    
    private <T> T parseError(Exception e, T defaultValue) {
        return defaultValue;
    }
    
    private <T> T parseError(Exception e) {
        return null;
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
