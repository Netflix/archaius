package com.netflix.archaius.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of {@link ParameterizedType} that can represent the collection types that Archaius can
 * handle with the default property value decoders, plus static utility methods for list, set and map types.
 *
 * @see PropertyRepository#getList(String, Class)
 * @see PropertyRepository#getSet(String, Class)
 * @see PropertyRepository#getMap(String, Class, Class)
 * @see Config#get(Type, String)
 * @see Config#get(Type, String, Object)
 */
public final class ArchaiusType implements ParameterizedType {

    /** Return a parameterizedType to represent a {@code List<listValuesType>} */
    public static ParameterizedType forListOf(Class<?> listValuesType) {
        Class<?> maybeWrappedType = listValuesType.isPrimitive() ? PRIMITIVE_WRAPPERS.getOrDefault(listValuesType, listValuesType) : listValuesType;
        return new ArchaiusType(List.class, new Class<?>[] { maybeWrappedType });
    }

    /** Return a parameterizedType to represent a {@code Set<setValuesType>} */
    public static ParameterizedType forSetOf(Class<?> setValuesType) {
        Class<?> maybeWrappedType = setValuesType.isPrimitive() ? PRIMITIVE_WRAPPERS.getOrDefault(setValuesType, setValuesType) : setValuesType;
        return new ArchaiusType(Set.class, new Class<?>[] { maybeWrappedType });
    }

    /** Return a parameterizedType to represent a {@code Map<mapKeysType, mapValuesType>} */
    public static ParameterizedType forMapOf(Class<?> mapKeysType, Class<?> mapValuesType) {
        Class<?> maybeWrappedKeyType = mapKeysType.isPrimitive() ? PRIMITIVE_WRAPPERS.getOrDefault(mapKeysType, mapKeysType) : mapKeysType;
        Class<?> maybeWrappedValuesType = mapValuesType.isPrimitive() ? PRIMITIVE_WRAPPERS.getOrDefault(mapValuesType, mapValuesType) : mapValuesType;

        return new ArchaiusType(Map.class, new Class<?>[] {maybeWrappedKeyType, maybeWrappedValuesType});
    }

    private final static Map<Class<?> /*primitive*/, Class<?> /*wrapper*/> PRIMITIVE_WRAPPERS;
    static {
        Map<Class<?>, Class<?>> wrappers = new HashMap<>();
        wrappers.put(Integer.TYPE, Integer.class);
        wrappers.put(Long.TYPE, Long.class);
        wrappers.put(Double.TYPE, Double.class);
        wrappers.put(Float.TYPE, Float.class);
        wrappers.put(Boolean.TYPE, Boolean.class);
        wrappers.put(Character.TYPE, Character.class);
        wrappers.put(Byte.TYPE, Byte.class);
        wrappers.put(Short.TYPE, Short.class);
        wrappers.put(Void.TYPE, Void.class);

        PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(wrappers);
    }

    private final Class<?> rawType;
    private final Class<?>[] typeArguments;

    private ArchaiusType(Class<?> rawType, Class<?>[] typeArguments) {
        this.rawType = Objects.requireNonNull(rawType);
        this.typeArguments = Objects.requireNonNull(typeArguments);
        if (rawType.isArray()
            || rawType.isPrimitive()
            || rawType.getTypeParameters().length != typeArguments.length) {
            throw new IllegalArgumentException("The provided rawType and arguments don't look like a supported parameterized type");
        }
    }

    @Override
    public Type[] getActualTypeArguments() {
        return typeArguments.clone();
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(rawType.getName());
        sb.append('<');
        boolean first = true;
        for (Type t : typeArguments) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(t.getTypeName());
            first = false;
        }
        sb.append('>');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(typeArguments) ^ rawType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType other = (ParameterizedType) obj;
        return other.getOwnerType() == null &&
                Objects.equals(rawType, other.getRawType()) &&
                Arrays.equals(typeArguments, other.getActualTypeArguments());
    }
}
