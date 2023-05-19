package com.netflix.archaius.api;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Contract for a raw source of properties
 */
public interface PropertySource {
    /**
     * Get the raw property value.  No interpolation or other modification is done to the property.
     * @param key
     */
    default Optional<Object> getProperty(String key) { return Optional.empty(); }

    /**
     * Get the raw property value, but do not record any usage data.
     * @param key
     */
    default Optional<Object> getPropertyUninstrumented(String key) {
        return getProperty(key);
    }

    /**
     * Mechanism for consuming all properties of the PropertySource
     * @param consumer
     */
    default void forEachProperty(BiConsumer<String, Object> consumer) {}

    /**
     * Mechanism for consuming all properties of the PropertySource that also avoids any usage tracking
     * @param consumer
     */
    default void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
        forEachProperty(consumer);
    }

    /**
     * @return Name used to identify the source such as a filename.
     */
    default String getName() { return "unnamed"; }

    /**
     * @return True if empty or false otherwise.
     */
    boolean isEmpty();
}
