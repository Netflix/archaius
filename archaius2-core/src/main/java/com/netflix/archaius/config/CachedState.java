package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;

import java.util.Collections;
import java.util.Map;

/** Represents an immutable, current view of a dependent config over its parent configs. */
class CachedState {
    private final Map<String, Object> data;
    private final Map<String, Config> instrumentedKeys;

    CachedState(Map<String, Object> data, Map<String, Config> instrumentedKeys) {
        this.data = Collections.unmodifiableMap(data);
        this.instrumentedKeys = Collections.unmodifiableMap(instrumentedKeys);
    }

    Map<String, Object> getData() {
        return data;
    }

    Map<String, Config> getInstrumentedKeys() {
        return instrumentedKeys;
    }
}
