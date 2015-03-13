package com.netflix.archaius.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Config backed by an immutable map.
 */
public class MapConfig extends AbstractConfig {

    /**
     * The builder only provides convenience for fluent style adding of properties
     * 
     * {@code
     * <pre>
     * MapConfig.builder()
     *      .put("foo", "bar")
     *      .put("baz", 123)
     *      .build()
     * </pre>
     * }
     * @author elandau
     */
    public static class Builder {
        final String name;
        Map<String, String> map = new HashMap<String, String>();
        
        public Builder(String name) {
            this.name = name;
        }
        
        public <T> Builder put(String key, T value) {
            map.put(key, value.toString());
            return this;
        }
        
        public MapConfig build() {
            return new MapConfig(name, map);
        }
    }
    
    public static Builder builder(String name) {
        return new Builder(name);
    }
    
    private Map<String, String> props = new HashMap<String, String>();
    
    /**
     * Construct a MapConfig as a copy of the provided Map
     * @param name
     * @param props
     */
    public MapConfig(String name, Map<String, String> props) {
        super(name);
        this.props.putAll(props);
        this.props = Collections.unmodifiableMap(this.props);
    }

    /**
     * Construct a MapConfig as a copy of the provided properties
     * @param name
     * @param props
     */
    public MapConfig(String name, Properties props) {
        super(name);
        
        for (Entry<Object, Object> entry : props.entrySet()) {
            this.props.put(entry.getKey().toString(), entry.getValue().toString());
        }
        this.props = Collections.unmodifiableMap(this.props);
    }
    
    @Override
    public String getRawString(String key) {
        return props.get(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return props.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return props.isEmpty();
    }

    @Override
    public Iterator<String> getKeys() {
        return props.keySet().iterator();
    }

}
