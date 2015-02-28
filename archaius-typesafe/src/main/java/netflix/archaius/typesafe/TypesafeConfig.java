package netflix.archaius.typesafe;

import netflix.archaius.config.AbstractConfig;

import com.typesafe.config.Config;

public class TypesafeConfig extends AbstractConfig {

    private final Config config;
    
    public TypesafeConfig(String name, Config config) {
        super(name);
        this.config = config;
    }

    @Override
    public boolean containsProperty(String key) {
        return config.hasPath(key);
    }

    @Override
    public int size() {
        return config.entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public String getProperty(String key) {
        return config.getString(key);
    }
}
