package netflix.archaius.config;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
        
        addConfigsLast(config);
    }

    /**
     * Add a Config to the end of the list so that it has least priority
     * @param config
     * @return
     */
    public CompositeConfig addConfigLast(Config config) {
        if (config == null) {
            return this;
        }
        levels.add(config);
        config.setStrInterpolator(this.getStrInterpolator());
        return this;
    }
    
    /**
     * Add a Config to the end of the list so that it has highest priority
     * @param config
     * @return
     */
    public CompositeConfig addConfigFirst(Config config) {
        if (config == null) {
            return this;
        }
        levels.add(0, config);
        config.setStrInterpolator(this.getStrInterpolator());
        return this;
    }
    
    public CompositeConfig addConfigsLast(Collection<Config> config) {
        levels.addAll(config);
        return this;
    }
    
    public CompositeConfig addConfigsFirst(Collection<Config> config) {
        levels.addAll(0, config);
        return this;
    }
    
    public CompositeConfig removeConfig(Config config) {
        this.levels.remove(config);
        return this;
    }
    
    /**
     * Compact all the configurations into a single config
     */
    public void compact() {
    }
    
    @Override
    public Object getRawProperty(String key) {
        for (Config config : levels) {
            if (config.containsProperty(key)) {
                return config.getRawProperty(key);
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
    public boolean isEmpty() {
        for (Config config : levels) {
            if (!config.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public Iterator<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Config config : levels) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                result.add(key);
            }
        }
        return result.iterator();
    }
}
