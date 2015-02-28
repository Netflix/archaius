package netflix.archaius.config;

import java.util.Properties;

public class SystemConfig extends AbstractConfig {

    private static final String NAME = "SYSTEM";
    
    private final Properties props;
    
    public SystemConfig() {
        super(NAME);
        props = System.getProperties();
    }

    @Override
    public String getProperty(String key) {
        return props.getProperty(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return props.containsKey(key);
    }

    @Override
    public int size() {
        return props.size();
    }

    @Override
    public boolean isEmpty() {
        return props.isEmpty();
    }

}
