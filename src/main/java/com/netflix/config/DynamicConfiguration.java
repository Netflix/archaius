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

public class DynamicConfiguration extends ConcurrentMapConfiguration {
    private final AbstractPollingScheduler scheduler;
    private final PolledConfigurationSource source;
                
    public DynamicConfiguration(PolledConfigurationSource source, AbstractPollingScheduler scheduler) {
        this.scheduler = scheduler;
        this.source = source;
        scheduler.startPolling(source, this);
    }
    
    public synchronized void stopLoading() {
        scheduler.stop();       
    }
    
    public PolledConfigurationSource getSource() {
        return source;
    }
}
