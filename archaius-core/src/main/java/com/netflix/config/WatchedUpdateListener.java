package com.netflix.config;

/**
 * The listener to be called when a {@link WatchedConfigurationSource} receives an update.
 * 
 * @author cfregly
 */
public interface WatchedUpdateListener {
    /**
     * Updates the configuration either incrementally or fully depending on the type of
     * {@link WatchedUpdateResult} that is passed.
     */
    public void updateConfiguration(WatchedUpdateResult result);
}
