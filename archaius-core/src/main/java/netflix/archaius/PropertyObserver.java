package netflix.archaius;

/**
 * Handler for property change notifications.  
 * 
 * @see {@link DefaultAppConfig} for usage example
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
    public void onChange(T value);
    
    /**
     * Notification that a property update failed
     * @param propName
     * @param error
     */
    public void onError(Throwable error);
}
