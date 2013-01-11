package com.netflix.config;

import java.util.Map;

/**
 * The definition of configuration source that brings dynamic changes to the configuration via watchers.
 * 
 * @author cfregly
 */
public interface WatchedConfigurationSource {
    /**
     * Add {@link ConfigurationUpdateListener} listener
     * 
     * @param l
     */
    public void addConfigurationUpdateListener(ConfigurationUpdateListener l);

    /**
     * Remove {@link ConfigurationUpdateListener} listener
     * 
     * @param l
     */
    public void removeConfigurationUpdateListener(ConfigurationUpdateListener l);

    /**
     * Get a snapshot of the latest configuration data.<BR>
     * 
     * Note: The correctness of this data is only as good as the underlying config source's view of the data.
     */
    public Map<String, Object> getCurrentData() throws Exception;
}
