package netflix.archaius.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import netflix.archaius.Config;

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
        Map<String, Object> map = new HashMap<String, Object>();
        
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
    
    private Map<String, Object> props = new HashMap<String, Object>();
    
    /**
     * Construct a MapConfig as a copy of the provided Map
     * @param name
     * @param props
     */
    public MapConfig(String name, Map<String, Object> props) {
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
            this.props.put(entry.getKey().toString(), entry.getValue());
        }
        this.props = Collections.unmodifiableMap(this.props);
    }

    /**
     * Construct a MapConfig with a single Map that is a union of the provied configs
     * where the last added value wins
     * @param name
     * @param configs
     */
    public MapConfig(String name, Collection<Config> configs) {
        super(name);
        
        for (Config config : configs) {
            Iterator<String> keys = config.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                props.put(key, config.getRawProperty(key));
            }
        }
        
        this.props = Collections.unmodifiableMap(this.props);
    }
    
    @Override
    public Object getRawProperty(String key) {
        Object obj = props.get(key);
        if (obj == null) {
            return null;
        }
        return obj;
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
