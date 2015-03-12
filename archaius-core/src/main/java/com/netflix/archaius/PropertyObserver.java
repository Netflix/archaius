package com.netflix.archaius;

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
     * @param value The new value for the property.
     */
    public void onChange(T value);
    
    /**
     * Notification that a property update failed
     * @param error
     */
    public void onError(Throwable error);
}
