package com.netflix.archaius.api.instrumentation;

import java.util.Map;

/** Instrumentation data snapshot for usages captured since the last flush. */
public class PropertiesInstrumentationData {
    private final Map<String, PropertyUsageData> idToUsageDataMap;

    public PropertiesInstrumentationData(Map<String, PropertyUsageData> idToUsageDataMap) {
        this.idToUsageDataMap = idToUsageDataMap;
    }

    public Map<String, PropertyUsageData> getIdToUsageDataMap() {
        return idToUsageDataMap;
    }
}
