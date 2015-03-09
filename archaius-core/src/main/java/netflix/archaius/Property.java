package netflix.archaius;

import java.util.concurrent.TimeUnit;

/**
 * API to access latest cached value for a Property.  When creating a Property object from the config manager
 * the property will auto register for change notifications. 
 * 
 * {@code 
 * class MyService {
 *     private final Property<String> prop;
 *     
 *     MyService(ConfigManager config) {
 *        prop = config.observe("foo.prop").asString("defaultValue");
 *     }
 *     
 *     void doSomething() {
 *         // Will print out the most up to date value for the property
 *         System.out.println(prop.get());
 *     }
 * }
 * }
 * 
 * @author elandau
 *
 * @param <T>
 */
public interface Property<T> {
    /**
     * @return  Most recent value for the property
     */
    T get(T defaultValue);
    
    /**
     * Get the last time the property was updated
     * @param units
     * @return
     */
    long getLastUpdateTime(TimeUnit units);
    
    /**
     * Unsubscribe from property value update notifications.  The property object cannot be resubscribed.
     */
    void unsubscribe();

    void addObserver(PropertyObserver<T> observer);

    void removeObserver(PropertyObserver<T> observer);
}
