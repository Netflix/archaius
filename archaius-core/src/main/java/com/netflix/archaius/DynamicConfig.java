package com.netflix.archaius;

/**
 * SPI for listening to DynamicConfig.  Note that this API does not and should not provide
 * any mechanism to set properties on the DynamicConfig.  Instead the concrete implementation
 * shall be responsible for refreshing the configuration.
 * 
 * @author elandau
 *
 */
public interface DynamicConfig extends Config {
    /**
     * Register a listener that will receive a call for each property that is added, removed
     * or updated.  It is recommended that the callbacks be invoked only after a full refresh
     * of the properties to ensure they are in a consistent state.
     * 
     * @param listener
     */
    void addListener(DynamicConfigObserver listener);

    /**
     * Remove a previously registered listener.
     * @param listener
     */
    void removeListener(DynamicConfigObserver listener);

}
