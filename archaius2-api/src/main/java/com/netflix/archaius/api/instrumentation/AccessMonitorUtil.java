package com.netflix.archaius.api.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Tracks property usage data and flushes the data periodically to a sink. */
public class AccessMonitorUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AccessMonitorUtil.class);

    private final ConcurrentHashMap<String, PropertyUsageData> propertyUsageMap;

    // A map from stack trace to how many times that stack trace appeared
    private final ConcurrentHashMap<String, Integer> stackTrace;

    private static final AtomicInteger counter = new AtomicInteger();

    private final ScheduledExecutorService executor;

    private final Consumer<PropertiesInstrumentationData> dataFlushConsumer;
    private final boolean recordStackTrace;

    public static class Builder {
        private Consumer<PropertiesInstrumentationData> dataFlushConsumer = null;
        private boolean recordStackTrace = false;

        public Builder setDataFlushConsumer(Consumer<PropertiesInstrumentationData> dataFlushConsumer) {
            this.dataFlushConsumer = dataFlushConsumer;
            return this;
        }

        public Builder setRecordStackTrace(boolean recordStackTrace) {
            this.recordStackTrace = recordStackTrace;
            return this;
        }

        public AccessMonitorUtil build() {
            return new AccessMonitorUtil(dataFlushConsumer, recordStackTrace);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private AccessMonitorUtil(Consumer<PropertiesInstrumentationData> dataFlushConsumer, boolean recordStackTrace) {
        this.propertyUsageMap = new ConcurrentHashMap<>();
        this.stackTrace = new ConcurrentHashMap<>();
        this.dataFlushConsumer = dataFlushConsumer;
        this.recordStackTrace = recordStackTrace;
        this.executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setDaemon(true);
                    thread.setName(String.format("Archaius-Instrumentation-Flusher-%d", counter.incrementAndGet()));
                    return thread;
                });
        startFlushing();
    }

    private void startFlushing() {
        if (!flushingEnabled()) {
            LOG.warn("Failed to start flushing Archaius instrumentation because flushing is not enabled.");
        } else {
            executor.scheduleWithFixedDelay(() -> {
                try {
                    if (flushingEnabled()) {
                        dataFlushConsumer.accept(new PropertiesInstrumentationData(getAndClearUsageMap()));
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to flush property instrumentation data", e);
                }
            }, 30, 120, TimeUnit.SECONDS);
        }
    }

    /** Merge the results of given accessMonitorUtil into this one. */
    public void merge(AccessMonitorUtil accessMonitorUtil) {
        for (Map.Entry<String, PropertyUsageData> entry : accessMonitorUtil.propertyUsageMap.entrySet()) {
            propertyUsageMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : accessMonitorUtil.stackTrace.entrySet()) {
            stackTrace.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public void registerUsage(PropertyDetails propertyDetails) {
        // Initially, we limit the number of events we keep to one event per property id per flush.
        propertyUsageMap.putIfAbsent(
                propertyDetails.getId(),
                new PropertyUsageData(createEventList(new PropertyUsageEvent(System.currentTimeMillis()))));

        // Very crude and will have a very noticeable performance impact, but is
        // particularly useful for finding out call sites that iterate over all
        // properties.
        if (recordStackTrace) {
            String trace = Arrays.toString(Thread.currentThread().getStackTrace());
            stackTrace.merge(trace, 1, (v1, v2) -> v1 + 1);
        }
    }

    private List<PropertyUsageEvent> createEventList(PropertyUsageEvent event) {
        List<PropertyUsageEvent> list = new ArrayList<>();
        list.add(event);
        return list;
    }

    private Map<String, PropertyUsageData> getAndClearUsageMap() {
        synchronized (propertyUsageMap) {
            Map<String, PropertyUsageData> ret = getUsageMapImmutable();
            propertyUsageMap.clear();
            return ret;
        }
    }

    public Map<String, PropertyUsageData> getUsageMapImmutable() {
        return Collections.unmodifiableMap(new HashMap<>(propertyUsageMap));
    }

    public Map<String, Integer> getStackTracesImmutable() {
        return Collections.unmodifiableMap(new HashMap<>(stackTrace));
    }

    public boolean flushingEnabled() {
        return dataFlushConsumer != null;
    }
}
