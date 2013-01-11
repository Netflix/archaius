package com.netflix.config;

/**
 * The listener to be called upon when a {@link WatchedConfigurationSource} completes a polling.
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
