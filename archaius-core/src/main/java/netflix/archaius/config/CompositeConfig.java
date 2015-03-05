package netflix.archaius.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import netflix.archaius.Config;
import netflix.archaius.exceptions.ConfigException;

/**
 * Config that is a composite of multiple configuration and as such doesn't track a 
 * configuration of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration that was added later.
 * 
 * @author elandau
 *
 * TODO: Optional cache of queried properties
 */
public class CompositeConfig extends AbstractConfig {
    public static interface Listener {
        void onConfigAdded(Config child);
    }

    public static interface CompositeVisitor {
        void visit(Config child);
    }
    
    private final CopyOnWriteArrayList<Config>  children  = new CopyOnWriteArrayList<Config>();
    private final Map<String, Config>           lookup    = new LinkedHashMap<String, Config>();
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<Listener>();
    
    public CompositeConfig(String name) {
        super(name);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }
    
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }
    
    public void notifyOnAddConfig(Config child) {
        for (Listener listener : listeners) {
            listener.onConfigAdded(child);
        }
    }
    
    /**
     * Add a Config to the end of the list so that it has least priority
     * @param child
     * @return
     */
    public synchronized void addConfigLast(Config child) throws ConfigException {
        if (child == null) {
            return;
        }
        if (lookup.containsKey(child.getName())) {
            throw new ConfigException("Configuration with name " + child.getName() + " already exists");
        }

        lookup.put(child.getName(), child);
        children.add(child);
        postConfigAdd(child);
    }
    
    /**
     * Add a Config to the end of the list so that it has highest priority
     * @param child
     * @return
     */
    public synchronized void addConfigFirst(Config child) throws ConfigException {
        if (child == null) {
            return;
        }
        if (lookup.containsKey(child.getName())) {
            throw new ConfigException("Configuration with name " + child.getName() + " already exists");
        }
        lookup.put(child.getName(), child);

        children.add(0, child);
        postConfigAdd(child);
    }
    
    public synchronized Collection<String> getChildConfigNames() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.lookup.keySet());
        return result;
    }
    
    protected void postConfigAdd(Config child) {
        child.setStrInterpolator(this.getStrInterpolator());
        child.setParent(this);
        notifyOnAddConfig(child);
    }
    
    public void addConfigsLast(Collection<Config> config) throws ConfigException {
        for (Config child : config) {
            addConfigLast(child);
        }
    }
    
    public void addConfigsFirst(Collection<Config> config) throws ConfigException {
        for (Config child : config) {
            addConfigFirst(child);
        }
    }
    
    public synchronized boolean replace(Config child) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getName().equals(child.getName())) {
                children.set(i, child);
                postConfigAdd(child);
                return true;
            }
        }
        return false;
    }
    
    public synchronized void removeConfig(Config child) {
        if (this.children.remove(child)) {
            this.lookup.remove(child.getName());
        }
    }    
    
    @Override
    public Object getRawProperty(String key) {
        for (Config child : children) {
            if (child.containsProperty(key)) {
                return child.getRawProperty(key);
            }
        }
        
        return null;
    }

    @Override
    public boolean containsProperty(String key) {
        for (Config child : children) {
            if (child.containsProperty(key)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (Config child : children) {
            if (!child.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public Iterator<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Config config : children) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                result.add(key);
            }
        }
        return result.iterator();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("[");
        
        for (Config child : children) {
            sb.append(child).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof CompositeVisitor) {
            CompositeVisitor cv = (CompositeVisitor)visitor;
            for (Config child : children) {
                cv.visit(child);
            }
        }
        else {
            super.accept(visitor);
        }
    }
}
