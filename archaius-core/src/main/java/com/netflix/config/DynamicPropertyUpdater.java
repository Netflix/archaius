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

import com.google.common.base.Splitter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.netflix.config.validation.ValidationException;

/**
 * Apply the {@link WatchedUpdateResult} to the configuration.<BR>
 * 
 * If the result is a full result from source, each property in the result is added/set in the configuration. Any
 * property that is in the configuration - but not in the result - is deleted if ignoreDeletesFromSource is false.<BR>
 * 
 * If the result is incremental, properties will be added and changed from the partial result in the configuration.
 * Deleted properties are deleted from configuration iff ignoreDeletesFromSource is false.
 * 
 * This code is shared by both {@link AbstractPollingScheduler} and {@link DynamicWatchedConfiguration}.
 */
public class DynamicPropertyUpdater {
    private static Logger logger = LoggerFactory.getLogger(DynamicPropertyUpdater.class);

    /**
     * Updates the properties in the config param given the contents of the result param.
     * 
     * @param result
     *            either an incremental or full set of data
     * @param config
     *            underlying config map
     * @param ignoreDeletesFromSource
     *            if true, deletes will be skipped
     */
    public void updateProperties(final WatchedUpdateResult result, final Configuration config,
            final boolean ignoreDeletesFromSource) {
        if (result == null || !result.hasChanges()) {
            return;
        }

        logger.debug("incremental result? [{}]", result.isIncremental());
        logger.debug("ignored deletes from source? [{}]", ignoreDeletesFromSource);

        if (!result.isIncremental()) {
            Map<String, Object> props = result.getComplete();
            if (props == null) {
                return;
            }
            for (Entry<String, Object> entry : props.entrySet()) {
                addOrChangeProperty(entry.getKey(), entry.getValue(), config);
            }
            Set<String> existingKeys = new HashSet<String>();
            for (Iterator<String> i = config.getKeys(); i.hasNext();) {
                existingKeys.add(i.next());
            }
            if (!ignoreDeletesFromSource) {
                for (String key : existingKeys) {
                    if (!props.containsKey(key)) {
                        deleteProperty(key, config);
                    }
                }
            }
        } else {
            Map<String, Object> props = result.getAdded();
            if (props != null) {
                for (Entry<String, Object> entry : props.entrySet()) {
                    addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            props = result.getChanged();
            if (props != null) {
                for (Entry<String, Object> entry : props.entrySet()) {
                    addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            if (!ignoreDeletesFromSource) {
                props = result.getDeleted();
                if (props != null) {
                    for (String name : props.keySet()) {
                        deleteProperty(name, config);
                    }
                }
            }
        }
    }

    /**
     * Add or update the property in the underlying config depending on if it exists
     * 
     * @param name
     * @param newValue
     * @param config
     */
    void addOrChangeProperty(final String name, final Object newValue, final Configuration config) {
        // We do not want to abort the operation due to failed validation on one property
        try {
            if (!config.containsKey(name)) {
                logger.debug("adding property key [{}], value [{}]", name, newValue);
    
                config.addProperty(name, newValue);
            } else {
                Object oldValue = config.getProperty(name);
             
                if (newValue != null) {
                    Object newValueArray;
                    if (oldValue instanceof CopyOnWriteArrayList && AbstractConfiguration.getDefaultListDelimiter() != '\0') {
                        newValueArray = new CopyOnWriteArrayList();

                        if (newValue != null && !(newValue instanceof String))
                        {
                            throw new ValidationException("Property " + name + " is expected to be a String, but "
                                    + newValue.getClass().getName() + " was received");
                        }
                        Iterable<String> stringiterator = Splitter.on(AbstractConfiguration.getDefaultListDelimiter()).omitEmptyStrings().trimResults().split((String) newValue);
                        for (String s : stringiterator) {
                            ((CopyOnWriteArrayList) newValueArray).add(s);
                        }
                    } else {
                        newValueArray = newValue;
                    }

                    if (!newValueArray.equals(oldValue)) {
                        logger.debug("updating property key [{}], value [{}]", name, newValue);
    
                        config.setProperty(name, newValue);
                    }

                } else if (oldValue != null) {
                    logger.debug("nulling out property key [{}]", name);
    
                    config.setProperty(name, null);
                }
            }
        } catch (ValidationException e) {
            logger.warn("Validation failed for property " + name, e);
        }
    }

    /**
     * Delete a property in the underlying config
     * 
     * @param key
     * @param config
     */
    void deleteProperty(final String key, final Configuration config) {
        if (config.containsKey(key)) {
            logger.debug("deleting property key [" + key + "]");

            config.clearProperty(key);
        }
    }
}