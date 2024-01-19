package com.netflix.archaius.instrumentation;

import com.netflix.archaius.api.PropertyDetails;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Tracks property usage data and flushes the data periodically to a sink. */
public class AccessMonitorUtil implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AccessMonitorUtil.class);

    // Map from property id to property usage data
    private final AtomicReference<ConcurrentHashMap<String, PropertyUsageData>> propertyUsageMapRef;

    // Map from stack trace to how many times that stack trace appeared
    private final ConcurrentHashMap<String, Integer> stackTrace;

    private static final AtomicInteger counter = new AtomicInteger();

    private final ScheduledExecutorService executor;

    private final Consumer<PropertiesInstrumentationData> dataFlushConsumer;
    private final boolean recordStackTrace;

    public static class Builder {
        private Consumer<PropertiesInstrumentationData> dataFlushConsumer = null;
        private boolean recordStackTrace = false;
        private int initialFlushDelaySeconds = 30;
        private int flushPeriodSeconds = 120;

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

        public AccessMonitorUtil build() {
            AccessMonitorUtil accessMonitorUtil = new AccessMonitorUtil(dataFlushConsumer, recordStackTrace);
            accessMonitorUtil.startFlushing(initialFlushDelaySeconds, flushPeriodSeconds);
            return accessMonitorUtil;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private AccessMonitorUtil(
            Consumer<PropertiesInstrumentationData> dataFlushConsumer,
            boolean recordStackTrace) {
        this.propertyUsageMapRef = new AtomicReference(new ConcurrentHashMap<>());
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
        for (Map.Entry<String, PropertyUsageData> entry : accessMonitorUtil.propertyUsageMapRef.get().entrySet()) {
            propertyUsageMapRef.get().putIfAbsent(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : accessMonitorUtil.stackTrace.entrySet()) {
            stackTrace.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public void registerUsage(PropertyDetails propertyDetails) {
        // Initially, we limit the number of events we keep to one event per property id per flush.
        propertyUsageMapRef.get().putIfAbsent(
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
        Map<String, PropertyUsageData> map =
                propertyUsageMapRef.getAndUpdate(unused -> new ConcurrentHashMap<>());
        return Collections.unmodifiableMap(new HashMap<>(map));
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
