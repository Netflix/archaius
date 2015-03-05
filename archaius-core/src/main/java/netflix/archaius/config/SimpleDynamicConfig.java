package netflix.archaius.config;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import netflix.archaius.Config;

public class SimpleDynamicConfig extends AbstractDynamicConfig implements SettableConfig {
    public SimpleDynamicConfig(String name) {
        super(name);
    }

    private ConcurrentMap<String, Object> props = new ConcurrentHashMap<String, Object>();
    
    @Override
    public void setProperty(String propName, Object propValue) {
        props.put(propName, propValue);
        notifyOnUpdate(propName);
    }
    
    @Override
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

    @Override
    public void setProperties(Properties properties) {
        if (null != properties) {
            for (Entry<Object, Object> prop : properties.entrySet()) {
                setProperty(prop.getKey().toString(), prop.getValue());
            }
        }
    }

}
