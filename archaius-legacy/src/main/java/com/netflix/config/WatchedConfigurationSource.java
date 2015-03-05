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
