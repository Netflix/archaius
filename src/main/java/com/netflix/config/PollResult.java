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
    
    public static PollResult createFull(Map<String, Object> complete) {
        return new PollResult(complete);        
    }
    
    public static PollResult createIncremental(Map<String, Object> added, 
            Map<String, Object> changed, Map<String, Object> deleted, Object checkPoint) {
        return new PollResult(added, changed, deleted, checkPoint);
    }
    
    public final boolean hasChanges() {
        if (incremental) {
            return  (added != null && added.size() > 0)
                || (changed != null && changed.size() > 0)
                || (deleted != null && deleted.size() > 0);  
        } else {
            return complete != null;
        }
    }

    public final Map<String, Object> getComplete() {
        return complete;
    }

    public final Map<String, Object> getAddedSinceLastPoll() {
        return added;
    }

    public final Map<String, Object> getChangedSinceLastPoll() {
        return changed;
    }

    public final Map<String, Object> getDeletedSinceLastPoll() {
        return deleted;
    }
    
    public final boolean isIncremental() {
        return incremental;
    }
    
    public final Object getCheckPoint() {
        return checkPoint;
    }
}
