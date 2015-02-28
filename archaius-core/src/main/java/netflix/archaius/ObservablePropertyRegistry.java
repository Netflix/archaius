package netflix.archaius;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ObservablePropertyRegistry implements PropertyListener {

    private final ConcurrentMap<String, ObservableProperty> registry = new ConcurrentHashMap<String, ObservableProperty>();
    private final ObservablePropertyFactory factory;
    
    public ObservablePropertyRegistry(ObservablePropertyFactory factory) {
        this.factory = factory;
    }
    
    @Override
    public void onUpdate(String key, Config config) {
        ObservableProperty property = registry.get(key);
        if (property != null) {
            property.reload();
        }
    }
    

    /**
     * Get the ObservableProperty for a specific property name.  The ObservableProperty
     * is cached internally.
     * 
     * @param key
     * @return
     */
    public ObservableProperty get(String key) {
        ObservableProperty observable = registry.get(key);
        if (observable == null) {
            observable = factory.create(key);
            ObservableProperty existing = registry.putIfAbsent(key, observable);
            if (existing != null) {
                return existing;
            }
        }
        
        return observable;
    }
}
