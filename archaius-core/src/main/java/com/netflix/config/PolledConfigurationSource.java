/**
 * Copyright 2013 Netflix, Inc.
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

/**
 * The definition of configuration source that brings dynamic changes to the configuration via polling.
 * 
 * @author awang
 */
public interface PolledConfigurationSource {

    /**
     * Poll the configuration source to get the latest content.
     * 
     * @param initial true if this operation is the first poll.
     * @param checkPoint Object that is used to determine the starting point if the result returned is incremental. 
     *          Null if there is no check point or the caller wishes to get the full content.
     * @return The content of the configuration which may be full or incremental.
     * @throws Exception If any exception occurs when fetching the configurations.
     */
    public PollResult poll(boolean initial, Object checkPoint) throws Exception;    
}
