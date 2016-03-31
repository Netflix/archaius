/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius.config.polling;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.api.config.PollingStrategy;
import com.netflix.archaius.util.Futures;
import com.netflix.archaius.util.ThreadFactories;

public class FixedPollingStrategy implements PollingStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(FixedPollingStrategy.class);
    private final ScheduledExecutorService executor;
    private final long interval;
    private final TimeUnit units;


    public FixedPollingStrategy(long interval, TimeUnit units) {
        this.executor = Executors.newSingleThreadScheduledExecutor(ThreadFactories.newNamedDaemonThreadFactory("Archaius-Poller-%d"));
        this.interval = interval;
        this.units    = units;

    }
    
    @Override
    public Future<?> execute(final Runnable callback) {
        while (true) {
            try {
                callback.run();
                break;
            } 
            catch (Exception e) {
                try {
                    LOG.warn("Fail to poll the polling source", e);
                    units.sleep(interval);
                } 
                catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return Futures.immediateFailure(e);
                }
            }
        }
        return executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.run();
                } catch (Exception e) {
                    LOG.warn("Failed to load properties", e);
                }
            }
        }, interval, interval, units);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

}
