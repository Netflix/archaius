package netflix.archaius.config;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import netflix.archaius.PropertyListener;
import netflix.archaius.DynamicConfig;

/**
 * Contract for a DynamicConfig source.
 * 
 * @author elandau
 */
public abstract class AbstractDynamicConfig extends AbstractConfig implements DynamicConfig {
    private CopyOnWriteArrayList<PropertyListener> listeners = new CopyOnWriteArrayList<PropertyListener>();
    
    public interface Listener {
        void onInvalidate(Collection<String> keys);
    }
    
    public AbstractDynamicConfig(String name) {
        super(name);
    }

    @Override
    public void addListener(PropertyListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(PropertyListener listener) {
        listeners.remove(listener);
    }
    
    protected void notifyOnUpdate(String key) {
        for (PropertyListener listener : listeners) {
            listener.onUpdate(key, this);
        }
    }
}
