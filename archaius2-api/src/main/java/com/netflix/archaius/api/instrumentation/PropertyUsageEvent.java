package com.netflix.archaius.api.instrumentation;

public class PropertyUsageEvent {
    private final long usageTimeMillis;

    public PropertyUsageEvent(long usageTimeMillis) {
        this.usageTimeMillis = usageTimeMillis;
    }

    public long getUsageTimeMillis() {
        return this.usageTimeMillis;
    }
}
