package com.netflix.archaius.config.polling;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.config.PollingStrategy;
import com.netflix.archaius.util.Futures;

public class FixedPollingStrategy implements PollingStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(FixedPollingStrategy.class);
    
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final long interval;
    private final TimeUnit units;
    private final boolean syncInit;
    
    public FixedPollingStrategy(long interval, TimeUnit units) {
        this(interval, units, true);
    }
    
    public FixedPollingStrategy(long interval, TimeUnit units, boolean syncInit) {
        this.interval = interval;
        this.units = units;
        this.syncInit = syncInit;
    }
    
    @Override
    public Future<?> execute(final Callable<Boolean> callback) {
        while (syncInit) {
            try {
                callback.call();
                break;
            } 
            catch (Exception e) {
                try {
                    units.sleep(interval);
                } 
                catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return Futures.immediateFailure(e);
                }
            }
        }
        return executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.call();
                } catch (Exception e) {
                    LOG.warn("Failed to load properties", e);
                }
            }
        }, interval, interval, units);
    }

}
