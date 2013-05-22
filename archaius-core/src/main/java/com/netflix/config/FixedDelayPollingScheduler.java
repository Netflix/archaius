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
 * A polling scheduler that schedule the polling with fixed delay. This class relies 
 * on java.util.concurrent.ScheduledExecutorService to do the scheduling.
 * 
 * @author awang
 */
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class FixedDelayPollingScheduler extends AbstractPollingScheduler {
    
    private ScheduledExecutorService executor;
    private int initialDelayMillis = 30000;
    private int delayMillis = 60000;
    
    /**
     * System property name to define the initial delay in milliseconds.
     */
    public static final String INITIAL_DELAY_PROPERTY = "archaius.fixedDelayPollingScheduler.initialDelayMills";
    
    /**
     * System property name to define the delay in milliseconds.
     */
    public static final String DELAY_PROPERTY = "archaius.fixedDelayPollingScheduler.delayMills";

    /**
     * Create an instance with initial delay and delay defined in system properties
     * {@value #INITIAL_DELAY_PROPERTY} and {@value #DELAY_PROPERTY}
     * The scheduler will delete the property in a configuration if it is absent from the configuration source.
     */
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
    
    /**
     * 
     * @param initialDelayMillis initial delay in milliseconds
     * @param delayMillis delay in milliseconds
     * @param ignoreDeletesFromSource whether the scheduler should ignore deletes of properties from configuration source when
     * applying the polling result to a configuration.
     */
    public FixedDelayPollingScheduler(int initialDelayMillis, int delayMillis, boolean ignoreDeletesFromSource) {
        super(ignoreDeletesFromSource);
        this.initialDelayMillis = initialDelayMillis;
        this.delayMillis = delayMillis;
    }
    
    /**
     * This method is implemented with 
     * {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
     */
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

