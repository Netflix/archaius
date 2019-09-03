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

/**
 * A configuration that polls a {@link PolledConfigurationSource} according to the schedule set by a
 * scheduler. The property values in this configuration will be changed dynamically at runtime if the
 * value changes in the configuration source.
 * <p>
 * This configuration does not allow null as key or value and will throw NullPointerException
 * when trying to add or set properties with empty key or value.
 * 
 * @author awang
 *
 */
public class DynamicConfiguration extends ConcurrentMapConfiguration {
    private AbstractPollingScheduler scheduler;
    private PolledConfigurationSource source;

    /**
     * Constant for the add property event type.
     */
    public static final int EVENT_RELOAD = 100;

    /**
     * Create an instance and start polling the configuration source.
     * 
     * @param source PolledConfigurationSource to poll
     * @param scheduler AbstractPollingScheduler whose {@link AbstractPollingScheduler#schedule(Runnable)} will be
     *                  used to determine the polling schedule
     */
    public DynamicConfiguration(PolledConfigurationSource source, AbstractPollingScheduler scheduler) {
        this();
        startPolling(source, scheduler);
    }
    
    public DynamicConfiguration() {
        super();
    }
    
    /**
     * Start polling the configuration source with the specified scheduler.
     * 
     * @param source PolledConfigurationSource to poll
     * @param scheduler AbstractPollingScheduler whose {@link AbstractPollingScheduler#schedule(Runnable)} will be
     *                  used to determine the polling schedule
     */
    public synchronized void startPolling(PolledConfigurationSource source, AbstractPollingScheduler scheduler) {
        this.scheduler = scheduler;
        this.source = source;
        init(source, scheduler);

        scheduler.addPollListener(new PollListener() {
            @Override
            public void handleEvent(EventType eventType, PollResult lastResult, Throwable exception) {
                switch (eventType) {
                    case POLL_SUCCESS:
                        fireEvent(EVENT_RELOAD, null, null, false);
                        break;
                    case POLL_FAILURE:
                        fireError(EVENT_RELOAD, null, null, exception);
                        break;
                    case POLL_BEGIN:
                        fireEvent(EVENT_RELOAD, null, null, true);
                        break;
                }
            }
        });

        scheduler.startPolling(source, this);
    }
    
    /**
     * Initialize the configuration. This method is called in 
     * {@link #DynamicConfiguration(PolledConfigurationSource, AbstractPollingScheduler)} 
     * and {@link #startPolling(PolledConfigurationSource, AbstractPollingScheduler)}
     * before the initial polling. The default implementation does nothing.
     */
    protected void init(PolledConfigurationSource source, AbstractPollingScheduler scheduler) {
    }
    
    /**
     * Stops the scheduler
     */
    public synchronized void stopLoading() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
    
    public PolledConfigurationSource getSource() {
        return source;
    }    
}
