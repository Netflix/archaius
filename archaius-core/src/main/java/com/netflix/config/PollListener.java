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
 * The listener to be called upon when {@link AbstractPollingScheduler} completes a polling.
 * 
 * @author awang
 *
 */
public interface PollListener {

    public enum EventType {
        POLL_SUCCESS, POLL_FAILURE
    }
    /**
     * This method is called when the listener is invoked after a polling.
     * 
     * @param eventType type of the event
     * @param lastResult the last poll result, null if the poll fails or there is no result 
     * @param exception any Throwable caught in the last poll, null if the poll is successful
     */
    public void handleEvent(EventType eventType, PollResult lastResult, Throwable exception);
}
