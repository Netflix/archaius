package netflix.archaius;

import java.util.concurrent.TimeUnit;

/**
 * API to access latest cached value for a Property.  A Property is created from a PropertyFactory
 * that is normally bound to a top level configuration object such ask {@link AppConfig}.  Through
 * a Property its also possible to receive a stream of property update notifications.
 * 
 * {@code 
 * class MyService {
 *     private final Property<String> prop;
 *     
 *     MyService(PropertyFactroy config) {
 *        prop = config.connectProperty("foo.prop").asString("defaultValue");
 *     }
 *     
 *     void doSomething() {
 *         // Will print out the most up to date value for the property
 *         System.out.println(prop.get());
 *     }
 * }
 * }
 * 
 * TODO: Chain properties
 * TODO: Property validator
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

    Property<T> addObserver(PropertyObserver<T> observer);

    void removeObserver(PropertyObserver<T> observer);
}
