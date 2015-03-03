package netflix.archaius;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

/**
 * Core API for accessing a configuration.  The API is readonly.
 * 
 * @author elandau
 */
public interface Config {
    /**
     * @return  Arbitrary name assigned to this configuration
     */
    String getName();
    
    Long getLong(String key);
    Long getLong(String key, Long defaultValue);

    String getString(String key);
    String getString(String key, String defaultValue);
    
    Double getDouble(String key);
    Double getDouble(String key, Double defaultValue);
    
    Integer getInteger(String key);
    Integer getInteger(String key, Integer defaultValue);
    
    Boolean getBoolean(String key);
    Boolean getBoolean(String key, Boolean defaultValue);
    
    Short getShort(String key);
    Short getShort(String key, Short defaultValue);
    
    BigInteger getBigInteger(String key);
    BigInteger getBigInteger(String key, BigInteger defaultValue);
    
    BigDecimal getBigDecimal(String key);
    BigDecimal getBigDecimal(String key, BigDecimal defaultValue);
    
    Float getFloat(String key);
    Float getFloat(String key, Float defaultValue);
    
    Byte getByte(String key);
    Byte getByte(String key, Byte defaultValue);
    
    List<?> getList(String key);
    List<?> getList(String key, List<?> defaultValue);
    
    <T> T get(Class<T> type, String key);
    <T> T get(Class<T> type, String key, T defaultValue);
    
    boolean containsProperty(String key);
    
    boolean isEmpty();
    
    Iterator<String> getKeys();
    
    Iterator<String> getKeys(String prefix);
    
    /**
     * Return the raw String value for a property
     * @param key
     * @return
     */
    Object getProperty(String key);
    
    /**
     * Return the interpolated String value for a property
     */
    Object interpolate(String key);
    
    /**
     * Return a subset of the configuration prefixed by a key.
     * 
     * @param key
     * @return
     */
    Config subset(String prefix);
    
    void setStrInterpolator(StrInterpolator interpolator);
}
