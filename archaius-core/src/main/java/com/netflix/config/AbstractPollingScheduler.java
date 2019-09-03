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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.PollListener.EventType;


/**
 * This class is responsible for scheduling the periodical polling of a configuration source and applying the 
 * polling result to a Configuration.
 * <p>
 * A subclass should supply the specific scheduling logic in {@link #schedule(Runnable)} and {@link #stop()}. 
 * 
 * @author awang
 *
 */
public abstract class AbstractPollingScheduler {
    private volatile boolean ignoreDeletesFromSource;
    private List<PollListener> listeners = new CopyOnWriteArrayList<PollListener>();
    private volatile Object checkPoint;
    private static Logger log = LoggerFactory.getLogger(AbstractPollingScheduler.class);
    private DynamicPropertyUpdater propertyUpdater = new DynamicPropertyUpdater();
    
    /**
     * @param ignoreDeletesFromSource true if deletes happened in the configuration source should be ignored 
     *                  by the Configuration. <b>Warning: </b>If both {@link PollResult#isIncremental()} 
     *                  and this parameter are false, any property in the configuration that is missing in the
     *                  polled result will be deleted once the PollResult is applied.
     *                   
     */
    public AbstractPollingScheduler(boolean ignoreDeletesFromSource) {
        this.ignoreDeletesFromSource = ignoreDeletesFromSource;
    }

    /**
     * Create an instance where <code>ignoreDeletesFromSource</code> is set to false. 
     * 
     * @see #AbstractPollingScheduler(boolean)
     */
    public AbstractPollingScheduler() {
        this.ignoreDeletesFromSource = false;
    }

    /**
     * Do an initial poll from the source and apply the result to the configuration.
     * 
     * @param source source of the configuration
     * @param config Configuration to apply the polling result
     * @throws RuntimeException if any error occurs in polling the configuration source
     */
    protected synchronized void initialLoad(final PolledConfigurationSource source, final Configuration config) {      
        PollResult result = null;
        try {
            fireEvent(EventType.POLL_BEGIN, null, null);
            result = source.poll(true, null);
            checkPoint = result.getCheckPoint();
            try {
                populateProperties(result, config);
            } catch (Exception e) {
                throw new RuntimeException("Unable to load Properties", e);
            }
            fireEvent(EventType.POLL_SUCCESS, result, null);
        } catch (Exception e) {
            fireEvent(EventType.POLL_FAILURE, null, e);
            throw new RuntimeException("Unable to load Properties source from " + source, e);
        }
    }
    
    /**
     * Apply the polled result to the configuration.
     * If the polled result is full result from source, each property in the result is either added to set 
     * to the configuration, and any property that is in the configuration but not in the result is deleted if ignoreDeletesFromSource
     * is false. If the polled result is incremental, properties added and changed in the partial result 
     * are set with the configuration, and deleted properties are deleted form configuration if ignoreDeletesFromSource
     * is false.
     * 
     * @param result Polled result from source
     */
    protected void populateProperties(final PollResult result, final Configuration config) {
        if (result == null || !result.hasChanges()) {
            return;
        }
        if (!result.isIncremental()) {
            Map<String, Object> props = result.getComplete();
            if (props == null) {
                return;
            }
            for (Entry<String, Object> entry: props.entrySet()) {
                propertyUpdater.addOrChangeProperty(entry.getKey(), entry.getValue(), config);
            }
            HashSet<String> existingKeys = new HashSet<String>();
            for (Iterator<String> i = config.getKeys(); i.hasNext();) {
                existingKeys.add(i.next());
            }
            if (!ignoreDeletesFromSource) {
                for (String key: existingKeys) {
                    if (!props.containsKey(key)) {
                        propertyUpdater.deleteProperty(key, config);
                    }
                }
            }
        } else {
            Map<String, Object> props = result.getAdded();
            if (props != null) {
                for (Entry<String, Object> entry: props.entrySet()) {
                    propertyUpdater.addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            props = result.getChanged();
            if (props != null) {
                for (Entry<String, Object> entry: props.entrySet()) {
                    propertyUpdater.addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            if (!ignoreDeletesFromSource) {
                props = result.getDeleted();
                if (props != null) {
                    for (String name: props.keySet()) {
                        propertyUpdater.deleteProperty(name, config);
                    }
                }            
            }
        }
    }
    
    /**
     * Gets the runnable to be scheduled. The implementation does the following
     * <li>Gets the next check point
     * <li>call source.poll(fase, checkpoint)
     * <li>fire event for poll listeners
     * <li>If success, update the configuration with the polled result
     * 
     * @return Runnable to be scheduled in {@link #schedule(Runnable)}
     */
    protected Runnable getPollingRunnable(final PolledConfigurationSource source, final Configuration config) {
        return new Runnable() {
            public void run() {
                log.debug("Polling started");
                PollResult result = null;
                try {
                    fireEvent(EventType.POLL_BEGIN, null, null);
                    result = source.poll(false, getNextCheckPoint(checkPoint));
                    checkPoint = result.getCheckPoint();

                    try {
                        populateProperties(result, config);
                    } catch (Exception e) {
                        log.error("Error applying properties", e);
                    }
                    fireEvent(EventType.POLL_SUCCESS, result, null);
                } catch (Exception e) {
                    log.error("Error getting result from polling source", e);
                    fireEvent(EventType.POLL_FAILURE, null, e);
                }
            }
            
        };   
    }

    private void fireEvent(PollListener.EventType eventType, PollResult result, Throwable e) {
        for (PollListener l: listeners) {
            try {
                l.handleEvent(eventType, result, e);
            } catch(Throwable ex) {
                log.error("Error in invoking listener", ex);
            }
        }
    }

    /**
     * Initiate the first poll of the configuration source and schedule the runnable. This may start a new thread or 
     * thread pool depending on the implementation of {@link #schedule(Runnable)}.
     * 
     * @param source Configuration source being polled
     * @param config Configuration where the properties will be updated
     * @throws RuntimeException if any error occurs in the initial polling
     */
    public void startPolling(final PolledConfigurationSource source, final Configuration config) {
        initialLoad(source, config);
        Runnable r = getPollingRunnable(source, config);
        schedule(r);
    }
    
    /**
     * Add the PollListener
     * 
     * @param l
     */
    public void addPollListener(PollListener l) {
        if (l!= null) {
            listeners.add(l);
        }
    }

    public void removePollListener(PollListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    /**
     * Get the check point used in next {@link PolledConfigurationSource#poll(boolean, Object)}. 
     * The check point can be used by the {@link PolledConfigurationSource} to determine 
     * the set of records to return. For example, a check point can be a time stamp and 
     * the {@link PolledConfigurationSource} can return the records modified since the time stamp.
     * This method is called before the poll. The 
     * default implementation returns the check point received from last poll.
     * 
     * @param lastCheckpoint checkPoint from last {@link PollResult#getCheckPoint()}
     * @return the check point to be used for the next poll
     */
    protected Object getNextCheckPoint(Object lastCheckpoint) {
        return lastCheckpoint;
    }

    /**
     * Schedule the runnable for polling the configuration source
     * 
     * @param pollingRunnable The runnable to be scheduled.
     */
    protected abstract void schedule(Runnable pollingRunnable);
    
    /**
     * Stop the scheduler
     */
    public abstract void stop();

    /**
     * @return if the scheduler ignores deletes from source
     */
    public final boolean isIgnoreDeletesFromSource() {
        return ignoreDeletesFromSource;
    }

    /**
     * Set if the scheduler should ignore deletes from source when applying property changes
     */
    public final void setIgnoreDeletesFromSource(boolean ignoreDeletesFromSource) {
        this.ignoreDeletesFromSource = ignoreDeletesFromSource;
    } 
    
}
