package com.netflix.config;

import java.util.Map;

/**
 * The definition of configuration source that brings dynamic changes to the configuration via watchers.
 * 
 * @author cfregly
 */
public interface WatchedConfigurationSource {
    /**
     * Add {@link WatchedUpdateListener} listener
     * 
     * @param l
     */
    public void addUpdateListener(WatchedUpdateListener l);

    /**
     * Remove {@link WatchedUpdateListener} listener
     * 
     * @param l
     */
    public void removeUpdateListener(WatchedUpdateListener l);

    /**
     * Get a snapshot of the latest configuration data.<BR>
     * 
     * Note: The correctness of this data is only as good as the underlying config source's view of the data.
     */
    public Map<String, Object> getCurrentData() throws Exception;
}
