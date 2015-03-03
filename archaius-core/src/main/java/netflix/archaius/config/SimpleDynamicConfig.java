package netflix.archaius.config;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import netflix.archaius.Config;

public class SimpleDynamicConfig extends AbstractDynamicConfig {
    public SimpleDynamicConfig(String name) {
        super(name);
    }

    private ConcurrentMap<String, Object> props = new ConcurrentHashMap<String, Object>();
    
    public void setProperty(String propName, String propValue) {
        props.put(propName, propValue);
        notifyOnUpdate(propName);
    }
    
    public void clearProperty(String propName) {
        props.remove(propName);
        notifyOnUpdate(propName);
    }

    public void appendConfig(Config config) {
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            props.put(key, config.getRawProperty(key));
        }
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
    public Object getRawProperty(String key) {
        return props.get(key);
    }
    
    @Override
    public Iterator<String> getKeys() {
        return props.keySet().iterator();
    }

}
