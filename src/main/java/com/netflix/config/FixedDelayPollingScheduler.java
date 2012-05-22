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


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

public class FixedDelayPollingScheduler extends AbstractPollingScheduler {
    
    private ScheduledExecutorService executor;
    private int initialDelayMillis = 30000;
    private int delayMillis = 60000;
    
    public static final String INITIAL_DELAY_PROPERTY = "fixedDelayPollingScheduler.initialDelayMills";

    public static final String DELAY_PROPERTY = "fixedDelayPollingScheduler.delayMills";

    public FixedDelayPollingScheduler() {
        String initialDelayProperty = System.getProperty(INITIAL_DELAY_PROPERTY);
        if (initialDelayProperty != null && initialDelayProperty.length() > 0) {
            initialDelayMillis = Integer.parseInt(initialDelayProperty);
        }
        String delayProperty = System.getProperty(DELAY_PROPERTY);
        if (delayProperty != null && delayProperty.length() > 0) {
            delayMillis = Integer.parseInt(delayProperty);
        }
    }
    
    public FixedDelayPollingScheduler(int initialDelayMillis, int delayMillis, boolean ignoreDeletesFromSource) {
        super(ignoreDeletesFromSource);
        this.initialDelayMillis = initialDelayMillis;
        this.delayMillis = delayMillis;
    }
    
    @Override
    protected synchronized void schedule(Runnable runnable) {
        executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "pollingConfigurationSource");
                t.setDaemon(true);
                return t;
            }
        });
        executor.scheduleWithFixedDelay(runnable, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);        
    }
        
    
    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}

