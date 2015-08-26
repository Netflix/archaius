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
package com.netflix.archaius;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

/**
 * Core API for reading a configuration.  The API is read only.
 * 
 * @author elandau
 */
public interface Config {
    public interface Visitor<T> {
        T visit(Config config, String key);
    }
    
    /**
     * Register a listener that will receive a call for each property that is added, removed
     * or updated.  It is recommended that the callbacks be invoked only after a full refresh
     * of the properties to ensure they are in a consistent state.
     * 
     * @param listener
     */
    void addListener(ConfigListener listener);

    /**
     * Remove a previously registered listener.
     * @param listener
     */
    void removeListener(ConfigListener listener);

    /**
     * Return the raw, uninterpolated, object associated with a key.
     * @param key
     */
    Object getRawProperty(String key);
    
    /**
     * Parse the property as a long.
     * @param key
     */
    Long getLong(String key);
    
    /**
     * Parse the property as a long but return a default if no property defined or the
     * property cannot be parsed successfully. 
     * @param key
     * @param defaultValue
     * @return
     */
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
    
    /**
     * Get the property as a list.  Depending on the underlying implementation the list
     * may be derived from a comma delimited string or from an actual list structure.
     * @param key
     * @return
     */
    List<?> getList(String key);
    
    <T> List<T> getList(String key, Class<T> type);
    
    List<?> getList(String key, List<?> defaultValue);

    /**
     * Get the property from the Decoder.  All basic data types as well any type
     * will a valueOf or String contructor will be supported.
     * @param type
     * @param key
     * @return
     */
    <T> T get(Class<T> type, String key);
    <T> T get(Class<T> type, String key, T defaultValue);
    
    /**
     * @param key
     * @return True if the key is contained within this or any of it's child configurations
     */
    boolean containsKey(String key);
    
    /**
     * @return True if empty or false otherwise.
     */
    boolean isEmpty();
    
    /**
     * @return Return an iterator to all property names owned by this config
     */
    Iterator<String> getKeys();
    
    /**
     * @return Return an interator to all prefixed property names owned by this config
     */
    Iterator<String> getKeys(String prefix);
    
    /**
     * @param prefix
     * @return Return a subset of the configuration prefixed by a key.
     */
    Config getPrefixedView(String prefix);
    
    /**
     * Set the interpolator to be used.  The interpolator is normally created from the top level
     * configuration object and is passed down to any children as they are added.
     * @param interpolator
     */
    void setStrInterpolator(StrInterpolator interpolator);
   
    StrInterpolator getStrInterpolator();
    
    /**
     * Set the Decoder used by get() to parse any type
     * @param decoder
     */
    void setDecoder(Decoder decoder);
    
    Decoder getDecoder();
    
    /**
     * Visitor pattern
     * @param visitor
     */
    <T> T accept(Visitor<T> visitor);
}
