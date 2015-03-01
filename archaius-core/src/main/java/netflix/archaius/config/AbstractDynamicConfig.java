package netflix.archaius.config;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import netflix.archaius.DynamicConfig;
import netflix.archaius.DynamicConfigObserver;

/**
 * Contract for a DynamicConfig source.
 * 
 * @author elandau
 */
public abstract class AbstractDynamicConfig extends AbstractConfig implements DynamicConfig {
    private CopyOnWriteArrayList<DynamicConfigObserver> listeners = new CopyOnWriteArrayList<DynamicConfigObserver>();
    
    public interface Listener {
        void onInvalidate(Collection<String> keys);
    }
    
    public AbstractDynamicConfig(String name) {
        super(name);
    }

    @Override
    public void addListener(DynamicConfigObserver listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(DynamicConfigObserver listener) {
        listeners.remove(listener);
    }
    
    protected void notifyOnUpdate(String key) {
        for (DynamicConfigObserver listener : listeners) {
            listener.onUpdate(key, this);
        }
    }
    
    protected void notifyOnUpdate() {
        for (DynamicConfigObserver listener : listeners) {
            listener.onUpdate(this);
        }
    }
    
    protected void notifyOnError(Throwable t) {
        for (DynamicConfigObserver listener : listeners) {
            listener.onError(t, this);
        }
    }
}
