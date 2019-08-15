package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Optional;

public final class ArrayTypeConverterFactory implements TypeConverter.Factory {
    public static final ArrayTypeConverterFactory INSTANCE = new ArrayTypeConverterFactory();

    private ArrayTypeConverterFactory() {}

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        Class clsType = (Class) type;

        if (clsType.isArray()) {
            TypeConverter elementConverter = registry.get(clsType.getComponentType()).orElseThrow(() -> new RuntimeException());
            return Optional.of(create(elementConverter, clsType.getComponentType()));
        }

        return Optional.empty();
    }

    private static TypeConverter<?> create(TypeConverter elementConverter, Class type) {
        return value -> {
            String[] elements = value.split(",");
            Object[] ar = (Object[]) Array.newInstance(type, elements.length);

            for (int i = 0; i < elements.length; i++) {
                ar[i] = elementConverter.convert(elements[i]);
            }
            return ar;
        };
    }
}
