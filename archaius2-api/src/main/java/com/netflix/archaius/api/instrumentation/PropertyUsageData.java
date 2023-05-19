package com.netflix.archaius.api.instrumentation;

import java.util.List;

public class PropertyUsageData {
    // AtomicInteger : counting usages

    private List<PropertyUsageEvent> propertyUsageEvents;
    public PropertyUsageData(List<PropertyUsageEvent> propertyUsageEvents) {
        this.propertyUsageEvents = propertyUsageEvents;
    }

    public List<PropertyUsageEvent> getPropertyUsageEvents() {
        return propertyUsageEvents;
    }

}
