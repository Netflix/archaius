package com.netflix.archaius.api;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Encapsulates conversion of a single string value to a type
 * @param <T>
 */
public interface TypeConverter<T> {
    /**
     * High level container from which to resolve a Type to a TypeConverter.  A repository normally contains
     * several {@link TypeConverter.Factory}s
     */
    interface Registry {
        Optional<TypeConverter<?>> get(Type type);
    }

    /**
     * Factory used to resolve a type to a TypeConverter.  Multiple factories may be used to support different
     * types, including generics.
     */
    interface Factory {
        Optional<TypeConverter<?>> get(Type type, Registry registry);
    }

    /**
     * Convert a string to the requested type
     * @param value
     * @return
     */
    T convert(String value);
}
