package com.netflix.archaius.api;

/**
 * Subscription created when adding a listener for a property.  The subscription's 
 * remove() method must be called to stop receiving property change updates.
 */
public interface ListenerSubscription {
    void unsubscribe();
}
