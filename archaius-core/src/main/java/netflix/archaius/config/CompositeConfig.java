package netflix.archaius.config;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import netflix.archaius.Config;

/**
 * Config that is a composite of multiple configuration and as such doesn't track a 
 * configuraiton of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration that was added later.
 * 
 * @author elandau
 *
 * TODO: Optional cache of queried properties
 */
public class CompositeConfig extends AbstractConfig {
    private final CopyOnWriteArrayList<Config> levels = new CopyOnWriteArrayList<Config>();

    public CompositeConfig(String name) {
        super(name);
    }

    public CompositeConfig(String name, Collection<Config> config) {
        super(name);
        
        addConfigs(config);
    }

    public CompositeConfig addConfig(Config config) {
        if (config == null) {
            return this;
        }
        levels.add(config);
        return this;
    }
    
    public CompositeConfig addConfigs(Collection<Config> config) {
        levels.addAll(config);
        return this;
    }
    
    public CompositeConfig removeConfig(Config config) {
        this.levels.remove(config);
        return this;
    }
    
    @Override
    public String getProperty(String key) {
        for (Config config : levels) {
            if (config.containsProperty(key)) {
                return config.getProperty(key);
            }
        }
        
        return null;
    }

    @Override
    public boolean containsProperty(String key) {
        for (Config config : levels) {
            if (config.containsProperty(key)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int size() {
        int size = 0;
        for (Config config : levels) {
            size += config.size();
        }
        
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (Config config : levels) {
            if (!config.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

}
