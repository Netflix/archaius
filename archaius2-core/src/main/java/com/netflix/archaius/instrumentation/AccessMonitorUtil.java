package com.netflix.archaius.instrumentation;

import com.netflix.archaius.api.PropertyDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Tracks property usage data and flushes the data periodically to a sink. */
public class AccessMonitorUtil implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AccessMonitorUtil.class);

    // Map from property id to property usage data
    private final AtomicReference<ConcurrentHashMap<String, PropertyUsageData>> propertyUsageMapRef;

    // Map from stack trace to how many times that stack trace appeared
    private final ConcurrentHashMap<String, Integer> stackTrace;
    // Property keys that we will keep the stack traces for
    private Set<String> propertiesToTrack;
    // Map from property key to stack traces map for tracked properties
    private final ConcurrentHashMap<String, Set<String>> trackedPropertyStackTraces;

    private static final AtomicInteger counter = new AtomicInteger();

    private final ScheduledExecutorService executor;

    private final Consumer<PropertiesInstrumentationData> dataFlushConsumer;
    private final boolean recordStackTrace;

    public static class Builder {
        private Consumer<PropertiesInstrumentationData> dataFlushConsumer = null;
        private boolean recordStackTrace = false;
        private int initialFlushDelaySeconds = 30;
        private int flushPeriodSeconds = 120;
        private Set<String> propertiesToTrack = Collections.emptySet();

        public Builder setDataFlushConsumer(Consumer<PropertiesInstrumentationData> dataFlushConsumer) {
            this.dataFlushConsumer = dataFlushConsumer;
            return this;
        }

        public Builder setRecordStackTrace(boolean recordStackTrace) {
            this.recordStackTrace = recordStackTrace;
            return this;
        }

        public Builder setInitialFlushDelaySeconds(int initialFlushDelaySeconds) {
            this.initialFlushDelaySeconds = initialFlushDelaySeconds;
            return this;
        }

        public Builder setFlushPeriodSeconds(int flushPeriodSeconds) {
            this.flushPeriodSeconds = flushPeriodSeconds;
            return this;
        }

        public Builder setPropertiesToTrack(Set<String> propertiesToTrack) {
            this.propertiesToTrack = propertiesToTrack;
            return this;
        }

        public AccessMonitorUtil build() {
            AccessMonitorUtil accessMonitorUtil =
                    new AccessMonitorUtil(dataFlushConsumer, recordStackTrace, propertiesToTrack);
            accessMonitorUtil.startFlushing(initialFlushDelaySeconds, flushPeriodSeconds);
            return accessMonitorUtil;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private AccessMonitorUtil(
            Consumer<PropertiesInstrumentationData> dataFlushConsumer,
            boolean recordStackTrace,
            Set<String> propertiesToTrack) {
        this.propertyUsageMapRef = new AtomicReference(new ConcurrentHashMap<>());
        this.stackTrace = new ConcurrentHashMap<>();
        this.dataFlushConsumer = dataFlushConsumer;
        this.recordStackTrace = recordStackTrace;
        this.propertiesToTrack = propertiesToTrack;
        this.trackedPropertyStackTraces = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setDaemon(true);
                    thread.setName(String.format("Archaius-Instrumentation-Flusher-%d", counter.incrementAndGet()));
                    return thread;
                });
    }

    public void setPropertiesToTrack(Set<String> propertiesToTrack) {
        this.propertiesToTrack = propertiesToTrack;
    }

    public Map<String, Set<String>> getTrackedPropertyTraces() {
        return trackedPropertyStackTraces;
    }

    private void startFlushing(int initialDelay, int period) {
        if (flushingEnabled()) {
            LOG.info("Starting flushing property usage data in {} seconds and then every {} seconds after.",
                    initialDelay, period);
            executor.scheduleWithFixedDelay(this::flushUsageData, initialDelay, period, TimeUnit.SECONDS);
        }
    }

    private void flushUsageData() {
        try {
            if (flushingEnabled()) {
                dataFlushConsumer.accept(new PropertiesInstrumentationData(getAndClearUsageMap()));
            }
        } catch (Exception e) {
            LOG.warn("Failed to flush property instrumentation data", e);
        }
    }

    /** Merge the results of given accessMonitorUtil into this one. */
    public void merge(AccessMonitorUtil accessMonitorUtil) {
        Map<String, PropertyUsageData> myMap = propertyUsageMapRef.get();
        for (Map.Entry<String, PropertyUsageData> entry : accessMonitorUtil.propertyUsageMapRef.get().entrySet()) {
            myMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : accessMonitorUtil.stackTrace.entrySet()) {
            stackTrace.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<String, Set<String>> entry : accessMonitorUtil.trackedPropertyStackTraces.entrySet()) {
            trackedPropertyStackTraces.merge(
                    entry.getKey(),
                    entry.getValue(),
                    (oldSet, newSet) -> {
                         oldSet.addAll(newSet);
                         return oldSet;
                    });
        }
    }

    public void registerUsage(PropertyDetails propertyDetails) {
        // Initially, we limit the number of events we keep to one event per property id per flush.
        propertyUsageMapRef.get().putIfAbsent(
                propertyDetails.getId(),
                new PropertyUsageData(createEventList(new PropertyUsageEvent(System.currentTimeMillis()))));

        boolean isTrackedProperty = propertiesToTrack.contains(propertyDetails.getKey());
        if (recordStackTrace || isTrackedProperty) {
            String trace = Arrays.toString(Thread.currentThread().getStackTrace());

            // Very crude and will have a very noticeable performance impact, but is
            // particularly useful for finding out call sites that iterate over all
            // properties.
            if (recordStackTrace) {
                stackTrace.merge(trace, 1, (v1, v2) -> v1 + 1);
            }
            if (isTrackedProperty) {
                String key = propertyDetails.getKey();
                if (!trackedPropertyStackTraces.containsKey(key)) {
                    trackedPropertyStackTraces.put(key, ConcurrentHashMap.newKeySet());
                }
                trackedPropertyStackTraces.get(key).add(trace);
            }
        }
    }

    private List<PropertyUsageEvent> createEventList(PropertyUsageEvent event) {
        List<PropertyUsageEvent> list = new ArrayList<>();
        list.add(event);
        return list;
    }

    private Map<String, PropertyUsageData> getAndClearUsageMap() {
        Map<String, PropertyUsageData> map = propertyUsageMapRef.getAndSet(new ConcurrentHashMap<>());
        return Collections.unmodifiableMap(map);
    }

    public Map<String, PropertyUsageData> getUsageMapImmutable() {
        return Collections.unmodifiableMap(new HashMap<>(propertyUsageMapRef.get()));
    }

    public Map<String, Integer> getStackTracesImmutable() {
        return Collections.unmodifiableMap(new HashMap<>(stackTrace));
    }

    public boolean flushingEnabled() {
        return dataFlushConsumer != null;
    }

    @Override
    public void close() {
        executor.shutdown();
        flushUsageData();
    }
}
