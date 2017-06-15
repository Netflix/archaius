package com.netflix.archaius.api;

import java.util.Optional;
import java.util.function.BiConsumer;

public interface PropertySource {
    default Optional<Object> getProperty(String key) { return Optional.empty(); }

    default void forEach(BiConsumer<String, Object> consumer) {}
    
    default String getName() { return "unknown"; }

    /**
     * @return True if empty or false otherwise.
     */
    boolean isEmpty();
}
