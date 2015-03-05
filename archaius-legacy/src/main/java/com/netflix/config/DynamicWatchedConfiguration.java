/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class DynamicWatchedConfiguration extends ConcurrentMapConfiguration implements WatchedUpdateListener {
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
            WatchedUpdateResult result = WatchedUpdateResult.createFull(currentData);

            updateConfiguration(result);
        } catch (final Exception exc) {
            logger.error("could not getCurrentData() from the WatchedConfigurationSource", exc);
        }

        // add a listener for subsequent config updates
        this.source.addUpdateListener(this);
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
    public void updateConfiguration(final WatchedUpdateResult result) {
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