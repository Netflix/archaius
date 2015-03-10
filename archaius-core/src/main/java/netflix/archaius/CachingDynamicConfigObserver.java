package netflix.archaius;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingDynamicConfigObserver implements DynamicConfigObserver {

    private final ConcurrentMap<String, ObservableProperty> registry = new ConcurrentHashMap<String, ObservableProperty>();
    private final ObservablePropertyFactory factory;
    
    public CachingDynamicConfigObserver(ObservablePropertyFactory factory) {
        this.factory = factory;
    }
    
    @Override
    public void onUpdate(String key, Config config) {
        ObservableProperty property = registry.get(key);
        if (property != null) {
            property.update();
        }
    }
    
    @Override
    public void onError(Throwable error, Config config) {
        // TODO
    }

    /**
     * Get the ObservableProperty for a specific property name.  The ObservableProperty
     * is cached internally.
     * 
     * @param key
     * @return
     */
    public ObservableProperty create(String key) {
        ObservableProperty observable = registry.get(key);
        if (observable == null) {
            observable = factory.observeProperty(key);
            ObservableProperty existing = registry.putIfAbsent(key, observable);
            if (existing != null) {
                return existing;
            }
        }
        
        return observable;
    }

    public ObservableProperty get(String key) {
        return registry.get(key);
    }
    
    @Override
    public void onUpdate(Config config) {
        invalidate();
    }

    public void invalidate() {
        for (ObservableProperty prop : registry.values()) {
            prop.update();
        }
    }
}
