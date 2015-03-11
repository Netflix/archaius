package com.netflix.archaius;

/**
 * Listener for property updates.  Due to the cascading nature of property value resolution
 * there's not much value in specifying the value or differentiating between set, add and
 * delete.  Instead the listener is expected to fetch the property value from the config.
 * 
 * @author elandau
 */
public interface DynamicConfigObserver {
    /**
     * Notify the parent that the value of a key has changed.  The parent is expected
     * to refresh its complete state.
     * 
     * @param propName
     */
    public void onUpdate(String propName, Config config);

    /**
     * Notification that the entire DynamicConfig was updated.  Respond to this by 
     * invalidating the entire property registration cache as it is more efficient
     * than trying to determine the delta.
     * @param config
     */
    public void onUpdate(Config config);
    
    /**
     * Notify of an error in the configuration listener.  The error indicates that the
     * DyanmicConfig was not able to update its configuration.  The DynamicConfig
     * should maintain the last known good state
     * 
     * @param error
     */
    public void onError(Throwable error, Config config);
}
