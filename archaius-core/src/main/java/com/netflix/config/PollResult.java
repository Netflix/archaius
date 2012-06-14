/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config;

import java.util.Map;

/**
 * This class represents result from a poll of configuration source. The result may be the complete 
 * content of the configuration source, or an incremental one.
 * 
 * @author awang
 *
 */
public class PollResult {

    private Map<String, Object> complete, added, changed, deleted;
    private boolean incremental;
    private Object checkPoint;
    
    private PollResult() {
    }

    private PollResult(Map<String, Object> complete) {
        this.complete = complete;
        incremental = false;
    }
    
    private PollResult(Map<String, Object> added, Map<String, Object> changed, Map<String, Object> deleted, Object checkPoint) {
        this.added = added;
        this.changed = changed;
        this.deleted = deleted;
        this.checkPoint = checkPoint;
        incremental = true;
    }
    
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
     * Indicate whether this result has any content. If the result is incremental,
     * this is true if there is any any added, changed or deleted properties. If the result
     * is complete, this is true if {@link #getComplete()} is null.
     */
    public final boolean hasChanges() {
        if (incremental) {
            return  (added != null && added.size() > 0)
                || (changed != null && changed.size() > 0)
                || (deleted != null && deleted.size() > 0);  
        } else {
            return complete != null;
        }
    }

    /**
     * Get complete content from configuration source. null if the result is incremental.
     * 
     */
    public final Map<String, Object> getComplete() {
        return complete;
    }

    /**
     * @return the added properties in the configuration source as a map 
     */
    public final Map<String, Object> getAdded() {
        return added;
    }

    /**
     * @return the changed properties in the configuration source as a map 
     */
    public final Map<String, Object> getChanged() {
        return changed;
    }

    /**
     * @return the deleted properties in the configuration source as a map 
     */
    public final Map<String, Object> getDeleted() {
        return deleted;
    }
    
    /**
     * @return whether the result is incremental. false if it is the complete content of the configuration source.
     */
    public final boolean isIncremental() {
        return incremental;
    }
    
    /**
     * @return the check point (marker) for this poll result
     */
    public final Object getCheckPoint() {
        return checkPoint;
    }
}
