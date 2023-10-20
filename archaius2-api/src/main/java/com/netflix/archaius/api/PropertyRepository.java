package com.netflix.archaius.api;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PropertyRepository {
    /**
     * Fetch a property of a specific type.  A {@link Property} object is returned regardless of
     * whether a key for it exists in the backing configuration.  The {@link Property} is attached
     * to a dynamic configuration system and will have its value automatically updated
     * whenever the backing configuration is updated.  Fallback properties and default values
     * may be specified through the {@link Property} API.
     * <p>
     * This method does not handle polymorphic return types such as collections. Use {@link #get(String, Type)} or one
     * of the specialized utility methods in the interface for that case.
     * 
     * @param key   Property name
     * @param type  The type for the property value. This *can* be an array type, but not a primitive array
     *              (ie, you can use {@code Integer[].class} but not {@code int[].class})
     */
    <T> Property<T> get(String key, Class<T> type);

    /**
     * Fetch a property of a specific type.  A {@link Property} object is returned regardless of
     * whether a key for it exists in the backing configuration.  The {@link Property} is attached
     * to a dynamic configuration system and will have its value automatically updated
     * whenever the backing configuration is updated.  Fallback properties and default values
     * may be specified through the {@link Property} API.
     * <p>
     * Use this method to request polymorphic return types such as collections. See the utility methods in
     * {@link ArchaiusType} to get types for lists, sets and maps, or call the utility methods in this interface directly.
     *
     * @see ArchaiusType#forListOf(Class)
     * @see ArchaiusType#forSetOf(Class)
     * @see ArchaiusType#forMapOf(Class, Class)
     * @param key   Property name
     * @param type  Type of property value.
     */
    <T> Property<T> get(String key, Type type);

    /**
     * Fetch a property with a {@link List} value. This is just an utility wrapper around {@link #get(String, Type)}.
     * See that method's documentation for more details.
     */
    default <V> Property<List<V>> getList(String key, Class<V> listElementType) {
        return get(key, ArchaiusType.forListOf(listElementType));
    }

    /**
     * Fetch a property with a {@link Set} value. This is just an utility wrapper around {@link #get(String, Type)}.
     * See that method's documentation for more details.
     */
    default <V> Property<Set<V>> getSet(String key, Class<V> setElementType) {
        return get(key, ArchaiusType.forSetOf(setElementType));
    }

    /**
     * Fetch a property with a {@link Map} value. This is just an utility wrapper around {@link #get(String, Type)}.
     * See that method's documentation for more details.
     */
    default <K, V> Property<Map<K, V>> getMap(String key, Class<K> mapKeyType, Class<V> mapValueType) {
        return get(key, ArchaiusType.forMapOf(mapKeyType, mapValueType));
    }
}
