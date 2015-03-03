package netflix.archaius.config;

import java.util.Iterator;
import java.util.Properties;

public class SystemConfig extends AbstractConfig {

    private static final String DEFAULT_NAME = "SYSTEM";
    
    private final Properties props;
    
    public SystemConfig() {
        this(DEFAULT_NAME);
    }

    public SystemConfig(String name) {
        super(name);
        props = System.getProperties();
    }

    @Override
    public Object getRawProperty(String key) {
        return props.getProperty(key);
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
        return new Iterator<String>() {
            Iterator<Object> obj = props.keySet().iterator();
            
            @Override
            public boolean hasNext() {
                return obj.hasNext();
            }

            @Override
            public String next() {
                return obj.next().toString();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
