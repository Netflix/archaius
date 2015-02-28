package netflix.archaius.config;

import java.util.Map;

public class EnvironmentConfig extends AbstractConfig {

    private static final String NAME = "ENVIRONMENT";
    
    private Map<String, String> properties;
    
    public EnvironmentConfig() {
        super(NAME);
        this.properties = System.getenv();
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }
}
