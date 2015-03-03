package netflix.archaius.commons;

import java.util.Iterator;

import netflix.archaius.config.AbstractConfig;

import org.apache.commons.configuration.AbstractConfiguration;

/**
 * Adaptor to allow an Apache Commons Configuration AbstractConfig to be used
 * as an Archaius2 Config
 * 
 * @author elandau
 *
 */
public class CommonsToConfig extends AbstractConfig {

    private final AbstractConfiguration config;
    
    public CommonsToConfig(AbstractConfiguration config) {
        super("");
        this.config = config;
    }

    @Override
    public boolean containsProperty(String key) {
        return config.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        return config.getString(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }
}
