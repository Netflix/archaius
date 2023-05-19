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
package com.netflix.archaius.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.api.config.PollingStrategy;
import com.netflix.archaius.api.instrumentation.AccessMonitorUtil;
import com.netflix.archaius.api.instrumentation.PropertyDetails;
import com.netflix.archaius.config.polling.PollingResponse;

/**
 * Special DynamicConfig that reads an entire snapshot of the configuration
 * from a source and performs a delta comparison.  Each new snapshot becomes
 * the new immutable Map backing this config.  
 */
public class PollingDynamicConfig extends AbstractConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PollingDynamicConfig.class);
    
    private volatile Map<String, String> current = Collections.emptyMap();
    private volatile Map<String, String> currentIds = Collections.emptyMap();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Callable<PollingResponse> reader;
    private final AtomicLong updateCounter = new AtomicLong();
    private final AtomicLong errorCounter = new AtomicLong();
    private final PollingStrategy strategy;
    private AccessMonitorUtil accessMonitorUtil;
    private boolean instrumentationEnabled = false;

    public PollingDynamicConfig(Callable<PollingResponse> reader, PollingStrategy strategy) {
        this.reader = reader;
        this.strategy = strategy;
        strategy.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    update();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to poll configuration", e);
                }
            }
        });
    }

    @Override
    public boolean containsKey(String key) {
        return current.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return current.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        Object rawProperty = current.get(key);
        if (instrumentationEnabled() && rawProperty != null) {
            recordUsage(new PropertyDetails(key, currentIds.get(key), rawProperty));
        }
        return rawProperty;
    }

    @Override
    public Object getRawPropertyUninstrumented(String key) {
        return current.get(key);
    }

    private void update() throws Exception {
        // OK to ignore calls to update() if already busy updating 
        if (busy.compareAndSet(false, true)) {
            updateCounter.incrementAndGet();
            try {
                PollingResponse response = reader.call();
                if (response.hasData()) {
                    current = Collections.unmodifiableMap(response.getToAdd());
                    currentIds = Collections.unmodifiableMap(response.getNameToIdsMap());
                    notifyConfigUpdated(this);
                }
            }
            catch (Exception e) {
                LOG.trace("Error reading data from remote server ", e);
                
                errorCounter.incrementAndGet();
                try {
                    notifyError(e, this);
                }
                catch (Exception e2) {
                    LOG.warn("Failed to notify error observer", e2);
                }
                throw e;
            }
            finally {
                busy.set(false);
            }
        }
    }

    public void shutdown() {
        strategy.shutdown();
    }
    
    public long getUpdateCounter() {
        return updateCounter.get();
    }
    
    public long getErrorCounter() {
        return errorCounter.get();
    }
    
    @Override
    public Iterator<String> getKeys() {
        return current.keySet().iterator();
    }

    @Override
    public Iterable<String> keys() {
        return current.keySet();
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        boolean instrumentationEnabled = instrumentationEnabled();
        current.forEach((k, v) -> {
            if (instrumentationEnabled) {
                recordUsage(new PropertyDetails(k, currentIds.get(k), v));
            }
            consumer.accept(k, v);
        });
    }

    @Override
    public void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
        current.forEach(consumer);
    }

    public void setAccessMonitorUtil(AccessMonitorUtil accessMonitorUtil) {
        this.accessMonitorUtil = accessMonitorUtil;
        instrumentationEnabled = true;
    }

    // TODO: sdf
    public void disableInstrumentation() {
        this.instrumentationEnabled = false;
    }

    @Override
    public void recordUsage(PropertyDetails propertyDetails) {
        if (instrumentationEnabled()) {
            // Instrumentation calls from outside PollingDynamicConfig may not have ids populated, so we replace the id
            // here if the id isn't present.
            if (propertyDetails.getId() == null) {
                propertyDetails = new PropertyDetails(
                        propertyDetails.getKey(),
                        currentIds.get(propertyDetails.getKey()),
                        propertyDetails.getValue());
            }
            accessMonitorUtil.registerUsage(propertyDetails);
        }
    }

    @Override
    public boolean instrumentationEnabled() {
        return instrumentationEnabled && accessMonitorUtil != null;
    }
}
