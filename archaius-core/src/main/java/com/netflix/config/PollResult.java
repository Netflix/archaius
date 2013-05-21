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

import java.util.Map;

/**
 * This class represents result from a poll of configuration source. The result may be the complete 
 * content of the configuration source, or an incremental one.
 * 
 * @author awang
 * @author cfregly {@link WatchedUpdateResult}
 */
public class PollResult extends WatchedUpdateResult {
    protected final Object checkPoint;
    
    /**
     * Create a full result that represents the complete content of the configuration source.
     * @param complete map that contains all the properties
     */
    public static PollResult createFull(Map<String, Object> complete) {
        return new PollResult(complete);        
    }
    
    /**
     * Create a result that represents incremental changes from the configuration
     * source. 
     * 
     * @param added properties added 
     * @param changed properties changed
     * @param deleted properties deleted, in which case the value in the map will be ignored
     * @param checkPoint Object that served as a marker for this incremental change, for example, a time stamp
     *        of the last change. 
     */
    public static PollResult createIncremental(Map<String, Object> added, 
            Map<String, Object> changed, Map<String, Object> deleted, Object checkPoint) {
        return new PollResult(added, changed, deleted, checkPoint);
    }
    
    /**
     * @return the check point (marker) for this poll result
     */
    public final Object getCheckPoint() {
        return checkPoint;
    }
    
    PollResult(Map<String, Object> complete) {
        super(complete);
        this.checkPoint = null;
    }
    
    PollResult(Map<String, Object> added, Map<String, Object> changed, Map<String, Object> deleted, Object checkPoint) {
        super(added, changed, deleted);
        this.checkPoint = checkPoint;
    }
}
