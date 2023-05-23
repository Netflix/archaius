package com.netflix.archaius.api;

import java.util.Objects;

/**
 * Container class for any information about the property at usage time that is relevant for instrumentation purposes.
 */
public class PropertyDetails {
    private final String key;
    private final String id;
    private final Object value;
    public PropertyDetails(String key, String id, Object value) {
        this.key = key;
        this.id = id;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public Object getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PropertyDetails)) {
            return false;
        }
        PropertyDetails pd = (PropertyDetails) o;
        return Objects.equals(key, pd.key)
                && Objects.equals(id, pd.id)
                && Objects.equals(value, pd.value);
    }

    public String toString() {
        return "[key: " + key + ", id: " + id + ", value: " + value + "]";
    }
}
