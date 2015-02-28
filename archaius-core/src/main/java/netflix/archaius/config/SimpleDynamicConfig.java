package netflix.archaius.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleDynamicConfig extends AbstractDynamicConfig {
    public SimpleDynamicConfig(String name) {
        super(name);
    }

    private ConcurrentMap<String, String> data = new ConcurrentHashMap<String, String>();
    
    public void setProperty(String propName, String propValue) {
        data.put(propName, propValue);
        notifyOnUpdate(propName);
    }
    
    public void removeProperty(String propName) {
        data.remove(propName);
        notifyOnUpdate(propName);
    }

    @Override
    public boolean containsProperty(String key) {
        return data.containsKey(key);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public String getProperty(String key) {
        return data.get(key);
    }
}
