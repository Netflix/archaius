package com.netflix.archaius.api;

import java.lang.reflect.Type;

public interface PropertyRepository {
    /**
     * Fetch a property of a specific type.  A {@link Property} object is returned regardless of
     * whether a key for it exists in the backing configuration.  The {@link Property} is attached
     * to a dynamic configuration system and will have its value automatically updated
     * whenever the backing configuration is updated.  Fallback properties and default values
     * may be specified through the {@link Property} API.
     * 
     * @param key   Property name
     * @param type  Type of property value
     * @return
     */
    <T> Property<T> get(String key, Class<T> type);

    <T> Property<T> get(String key, Type type);
}
