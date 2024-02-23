package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;

import java.lang.reflect.Type;
import java.util.Optional;

public final class EnumTypeConverterFactory implements TypeConverter.Factory {
    public static final EnumTypeConverterFactory INSTANCE = new EnumTypeConverterFactory();

    private EnumTypeConverterFactory() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        if (type instanceof Class<?> && ((Class<?>) type).isEnum()) {
            Class enumClass = (Class<?>) type;
            return Optional.of(create(enumClass));
        }
        return Optional.empty();
    }

    private static <T extends Enum<T>> TypeConverter<T> create(Class<T> clsType) {
        return value -> Enum.valueOf(clsType, value);
    }
}
