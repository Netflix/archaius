package com.netflix.archaius.api.instrumentation;

/** Container for data about a single property usage event. */
public class PropertyUsageEvent {
    private final long usageTimeMillis;

    public PropertyUsageEvent(long usageTimeMillis) {
        this.usageTimeMillis = usageTimeMillis;
    }

    public long getUsageTimeMillis() {
        return this.usageTimeMillis;
    }
}
