package com.netflix.archaius;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.exceptions.ParseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A {@code Decoder} implementation that also implements {@code TypeConverter.Registry}, and delegates to a supplied
 * collection of converter factories.
 */
abstract class AbstractRegistryDecoder implements Decoder, TypeConverter.Registry {

    private final Map<Type, TypeConverter<?>> cache = new ConcurrentHashMap<>();

    private final List<TypeConverter.Factory> factories;

    AbstractRegistryDecoder(Collection<? extends TypeConverter.Factory> factories) {
        this.factories = Collections.unmodifiableList(new ArrayList<>(factories));
    }

    @Override
    public <T> T decode(Class<T> type, String encoded) {
        return decode((Type) type, encoded);
    }

    @Override
    public <T> T decode(Type type, String encoded) {
        try {
            if (encoded == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            TypeConverter<T> converter = (TypeConverter<T>) getOrCreateConverter(type);
            if (converter == null) {
                throw new RuntimeException("No converter found for type '" + type + "'");
            }
            return converter.convert(encoded);
        } catch (Exception e) {
            throw new ParseException("Error decoding type `" + type.getTypeName() + "`", e);
        }
    }

    @Override
    public Optional<TypeConverter<?>> get(Type type) {
        return Optional.ofNullable(getOrCreateConverter(type));
    }

    private TypeConverter<?> getOrCreateConverter(Type type) {
        TypeConverter<?> converter = cache.get(type);
        if (converter == null) {
            converter = resolve(type);
            if (converter == null) {
                return null;
            }
            TypeConverter<?> existing = cache.putIfAbsent(type, converter);
            if (existing != null) {
                converter = existing;
            }
        }
        return converter;
    }

    /**
     * Iterate through all TypeConverter#Factory's and return the first TypeConverter that matches
     * @param type
     * @return
     */
    private TypeConverter<?> resolve(Type type) {
        return factories.stream()
                .flatMap(factory -> factory.get(type, this).map(Stream::of).orElseGet(Stream::empty))
                .findFirst()
                .orElseGet(() -> findValueOfTypeConverter(type));
    }

    /**
     * @param type
     * @param <T>
     * @return Return a converter that uses reflection on either a static valueOf or ctor(String) to convert a string value to the
     *     type.  Will return null if neither is found
     */
    private static <T> TypeConverter<T> findValueOfTypeConverter(Type type) {
        if (!(type instanceof Class)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>) type;

        // Next look a valueOf(String) static method
        Method method;
        try {
            method = cls.getMethod("valueOf", String.class);
            return value -> {
                try {
                    return (T) method.invoke(null, value);
                } catch (Exception e) {
                    throw new ParseException("Error converting value '" + value + "' to '" + type.getTypeName() + "'", e);
                }
            };
        } catch (NoSuchMethodException e1) {
            // Next look for a T(String) constructor
            Constructor<T> c;
            try {
                c = cls.getConstructor(String.class);
                return value -> {
                    try {
                        return (T) c.newInstance(value);
                    } catch (Exception e) {
                        throw new ParseException("Error converting value", e);
                    }
                };
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
}
