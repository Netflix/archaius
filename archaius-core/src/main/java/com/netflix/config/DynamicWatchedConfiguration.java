package com.netflix.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration that waits for a watcher event from the specified config source.
 * 
 * The property values in this configuration will be changed dynamically at runtime if the value changes in the
 * underlying configuration source.
 * <p>
 * This configuration does not allow null as key or value and will throw NullPointerException when trying to add or set
 * properties with empty key or value.
 * 
 * @author cfregly
 */
@SuppressWarnings("unchecked")
public class DynamicWatchedConfiguration extends ConcurrentMapConfiguration implements ConfigurationUpdateListener {
    private final WatchedConfigurationSource source;
    private final boolean ignoreDeletesFromSource;
    private final DynamicPropertyUpdater updater;

    private static final Logger logger = LoggerFactory.getLogger(DynamicWatchedConfiguration.class);

    /**
     * Create an instance of the WatchedConfigurationSource, add listeners, and wait for the update callbacks.
     * 
     * @param source
     *            PolledConfigurationSource to poll
     */
    public DynamicWatchedConfiguration(WatchedConfigurationSource source, boolean ignoreDeletesFromSource,
            DynamicPropertyUpdater updater) {
        this.source = source;
        this.ignoreDeletesFromSource = ignoreDeletesFromSource;
        this.updater = updater;

        // get a current snapshot of the config source data
        try {
            Map<String, Object> currentData = source.getCurrentData();
            ConfigurationUpdateResult result = ConfigurationUpdateResult.createFull(currentData);

            updateConfiguration(result);
        } catch (final Exception exc) {
            logger.error("could not getCurrentData() from the WatchedConfigurationSource", exc);
        }

        // add a listener for subsequent config updates
        this.source.addConfigurationUpdateListener(this);
    }

    /**
     * Simplified constructor with the following defaults:<BR>
     *  ignoreDeletesFromSource = false<BR>
     *  dynamicPropertyUpdater = new {@link DynamicPropertyUpdater}()
     * 
     * @param source {@link WatchedConfigurationSource}
     */
    public DynamicWatchedConfiguration(final WatchedConfigurationSource source) {
        this(source, false, new DynamicPropertyUpdater());
    }

    @Override
    public void updateConfiguration(final ConfigurationUpdateResult result) {
        //Preconditions.checkNotNull(result);

        updater.updateProperties(result, this, ignoreDeletesFromSource);
    }

    /**
     * @return if the this configuration will ignore deletes from source
     */
    public boolean isIgnoreDeletesFromSource() {
        return ignoreDeletesFromSource;
    }

    /**
     * @return underlying {@link WatchedConfigurationSource}
     */
    public WatchedConfigurationSource getSource() {
        return source;
    }
}