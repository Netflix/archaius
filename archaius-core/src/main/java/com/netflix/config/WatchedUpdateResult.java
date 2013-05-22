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
 * This class represents the result of a callback from the WatchedConfigurationSource. The result may be the complete
 * content of the configuration source - or an incremental one.
 * 
 * @author cfregly
 * @author awang {@link PollResult)
 */
public class WatchedUpdateResult {
    protected final Map<String, Object> complete, added, changed, deleted;
    protected final boolean incremental;

    /**
     * Create a full result that represents the complete content of the configuration source.
     * 
     * @param complete
     *            map that contains all the properties
     */
    public static WatchedUpdateResult createFull(Map<String, Object> complete) {
        return new WatchedUpdateResult(complete);
    }

    /**
     * Create a result that represents incremental changes from the configuration source.
     * 
     * @param added
     *            properties added
     * @param changed
     *            properties changed
     * @param deleted
     *            properties deleted, in which case the value in the map will be ignored
     */
    public static WatchedUpdateResult createIncremental(Map<String, Object> added, Map<String, Object> changed,
            Map<String, Object> deleted) {
        return new WatchedUpdateResult(added, changed, deleted);
    }

    /**
     * Indicate whether this result has any content. If the result is incremental, this is true if there is any any
     * added, changed or deleted properties. If the result is complete, this is true if {@link #getComplete()} is null.
     */
     public boolean hasChanges() {
        if (incremental) {
            return (added != null && added.size() > 0) || (changed != null && changed.size() > 0)
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
    
    WatchedUpdateResult(Map<String, Object> complete) {
        this.complete = complete;
        this.added = null;
        this.changed = null;
        this.deleted = null;
        this.incremental = false;
    }

    WatchedUpdateResult(Map<String, Object> added, Map<String, Object> changed, Map<String, Object> deleted) {
        this.complete = null;
        this.added = added;
        this.changed = changed;
        this.deleted = deleted;
        this.incremental = true;
    }
}
