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

import com.netflix.config.sources.URLConfigurationSource;

/**
 * A {@link DynamicConfiguration} that uses a {@link URLConfigurationSource} and 
 * {@link FixedDelayPollingScheduler}.
 * 
 * @author awang
 *
 */
public class DynamicURLConfiguration extends DynamicConfiguration {
        
    /**
     * Create an instance with default {@link URLConfigurationSource#URLConfigurationSource()} and 
     * {@link FixedDelayPollingScheduler#FixedDelayPollingScheduler()} and start polling the source
     * if there is any URLs available for polling.
     */
    public DynamicURLConfiguration() {
        URLConfigurationSource source = new URLConfigurationSource();
        if (source.getConfigUrls() != null && source.getConfigUrls().size() > 0) {
            startPolling(source, new FixedDelayPollingScheduler());
        }
    }

    /**
     * Create an instance and start polling the source.
     * 
     * @param initialDelayMillis initial delay in milliseconds used by {@link FixedDelayPollingScheduler}
     * @param delayMillis delay interval in milliseconds used by {@link FixedDelayPollingScheduler}
     * @param ignoreDeletesFromSource whether the scheduler should ignore deletes of properties from configuration source when
     * applying the polling result to a configuration.
     * @param urls The set of URLs to be polled by {@link URLConfigurationSource}
     */
    public DynamicURLConfiguration(int initialDelayMillis, int delayMillis, boolean ignoreDeletesFromSource, 
            String... urls) {
        super(new URLConfigurationSource(urls),
                new FixedDelayPollingScheduler(initialDelayMillis, delayMillis, ignoreDeletesFromSource));
    }    
}
