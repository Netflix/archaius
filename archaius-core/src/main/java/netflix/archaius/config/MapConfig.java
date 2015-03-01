package netflix.archaius.config;

import java.util.HashMap;
import java.util.Map;

public class MapConfig extends AbstractConfig {

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
    
    private final Map<String, String> values = new HashMap<String, String>();
    
    public MapConfig(String name, Map<String, String> values) {
        super(name);
        this.values.putAll(values);
    }

    @Override
    public String getProperty(String key) {
        return values.get(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return values.containsKey(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

}
