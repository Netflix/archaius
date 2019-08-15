package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;

import java.lang.reflect.Type;
import java.util.Optional;

public final class EnumTypeConverterFactory implements TypeConverter.Factory {
    public static final EnumTypeConverterFactory INSTANCE = new EnumTypeConverterFactory();

    private EnumTypeConverterFactory() {}

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        Class clsType = (Class) type;

        if (clsType.isEnum()) {
            return Optional.of(create(clsType));
        }

        return Optional.empty();
    }

    private static TypeConverter<?> create(Class clsType) {
        return value -> Enum.valueOf(clsType, value);
    }
}
