package netflix.archaius;

/**
 * Handler for property change notifications.  
 * 
 * @see {@link ConfigManager} for usage example
 * 
 * @author elandau
 *
 * @param <T>
 */
public interface PropertyObserver<T> {
    /**
     * Notification that the property value changed.  next=null indicates that the property
     * has been deleted.
     * 
     * @param propName
     * @param prevValue
     * @param newValue
     */
    public void onChange(String propName, T prevValue, T newValue);
}
