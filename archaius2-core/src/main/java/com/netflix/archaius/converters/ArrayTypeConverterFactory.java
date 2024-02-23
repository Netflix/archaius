package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.ObjIntConsumer;

public final class ArrayTypeConverterFactory implements TypeConverter.Factory {
    public static final ArrayTypeConverterFactory INSTANCE = new ArrayTypeConverterFactory();

    private ArrayTypeConverterFactory() {}

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        if (type instanceof Class<?> && ((Class<?>) type).isArray()) {
            Class<?> clsType = (Class<?>) type;
            Class<?> elementType = clsType.getComponentType();
            @SuppressWarnings("unchecked")
            TypeConverter<Object> elementConverter = (TypeConverter<Object>) registry.get(elementType)
                    .orElseThrow(() -> new RuntimeException("No converter found for array element type '" + elementType + "'"));
            return Optional.of(create(elementConverter, clsType.getComponentType()));
        }

        return Optional.empty();
    }

    private static TypeConverter<?> create(TypeConverter<Object> elementConverter, Class<?> type) {
        return value -> {
            value = value.trim();
            if (value.isEmpty()) {
                return Array.newInstance(type, 0);
            }
            String[] elements = value.split(",");
            Object resultArray = Array.newInstance(type, elements.length);

            final ObjIntConsumer<String> elementHandler;
            if (type.isPrimitive()) {
                if (type.equals(int.class)) {
                    elementHandler = (s, idx) -> Array.setInt(resultArray, idx, (int) elementConverter.convert(s));
                } else if (type.equals(long.class)) {
                    elementHandler = (s, idx) -> Array.setLong(resultArray, idx, (long) elementConverter.convert(s));
                } else if (type.equals(short.class)) {
                    elementHandler = (s, idx) -> Array.setShort(resultArray, idx, (short) elementConverter.convert(s));
                } else if (type.equals(byte.class)) {
                    elementHandler = (s, idx) -> Array.setByte(resultArray, idx, (byte) elementConverter.convert(s));
                } else if (type.equals(char.class)) {
                    elementHandler = (s, idx) -> Array.setChar(resultArray, idx, (char) elementConverter.convert(s));
                } else if (type.equals(boolean.class)) {
                    elementHandler = (s, idx) -> Array.setBoolean(resultArray, idx, (boolean) elementConverter.convert(s));
                } else if (type.equals(float.class)) {
                    elementHandler = (s, idx) -> Array.setFloat(resultArray, idx, (float) elementConverter.convert(s));
                } else if (type.equals(double.class)) {
                    elementHandler = (s, idx) -> Array.setDouble(resultArray, idx, (double) elementConverter.convert(s));
                } else {
                    throw new UnsupportedOperationException("Unknown primitive type: " + type);
                }
            } else {
                elementHandler = (s, idx) -> Array.set(resultArray, idx, elementConverter.convert(s));
            }

            for (int i = 0; i < elements.length; i++) {
                elementHandler.accept(elements[i], i);
            }
            return resultArray;
        };
    }
}
