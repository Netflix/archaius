package com.netflix.archaius.api.instrumentation;

import java.util.List;

/** Container for all usages of a specific property in a flush cycle. */
public class PropertyUsageData {
    private List<PropertyUsageEvent> propertyUsageEvents;
    public PropertyUsageData(List<PropertyUsageEvent> propertyUsageEvents) {
        this.propertyUsageEvents = propertyUsageEvents;
    }

    public List<PropertyUsageEvent> getPropertyUsageEvents() {
        return propertyUsageEvents;
    }
}
