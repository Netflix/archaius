package com.netflix.config;

/**
 * The listener to be called when a {@link WatchedConfigurationSource} receives an update.
 * 
 * @author cfregly
 */
public interface ConfigurationUpdateListener {
    /**
     * Updates the configuration either incrementally or fully depending on the type of
     * {@link ConfigurationUpdateResult} that is passed.
     */
    public void updateConfiguration(ConfigurationUpdateResult result);
}
