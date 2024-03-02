package com.netflix.archaius.api;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ArchaiusTypeTest {

    private static final Type listOfString = new TypeToken<List<String>>() {}.getType();
    private static final Type setOfLong = new TypeToken<Set<Long>>() {}.getType();
    private static final Type mapOfIntToCharSequence = new TypeToken<Map<Integer, CharSequence>>() {}.getType();

    private static <T> TypeToken<List<T>> listOfType(Class<T> klazz) {
        return new TypeToken<List<T>>() {}.where(new TypeParameter<T>() {}, klazz);
    }

    @Test
    public void testEquals() {
        ParameterizedType archaiusType = ArchaiusType.forListOf(String.class);
        assertEquals(archaiusType, listOfString);
        assertEquals(listOfString, archaiusType);
        assertEquals(archaiusType, ArchaiusType.forListOf(String.class));
        assertNotEquals(archaiusType, ArchaiusType.forListOf(Integer.class));
        assertNotEquals(archaiusType, setOfLong);

        // Test against Guava's ParameterizedType implementation
        assertEquals(archaiusType, listOfType(String.class).getType());
    }

    @Test
    public void testHashCode() {
        assertEquals(listOfString.hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        assertEquals(ArchaiusType.forListOf(String.class).hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        assertEquals(setOfLong.hashCode(), ArchaiusType.forSetOf(Long.class).hashCode());
        assertEquals(ArchaiusType.forMapOf(Integer.class, CharSequence.class).hashCode(), mapOfIntToCharSequence.hashCode());

        // Test against Guava's ParameterizedType implementation
        assertEquals(ArchaiusType.forListOf(String.class).hashCode(), listOfType(String.class).getType().hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("java.util.List<java.lang.String>", ArchaiusType.forListOf(String.class).toString());
        assertEquals(listOfString.toString(), ArchaiusType.forListOf(String.class).toString());
        assertEquals(setOfLong.toString(), ArchaiusType.forSetOf(Long.class).toString());
        assertEquals(mapOfIntToCharSequence.toString(), ArchaiusType.forMapOf(Integer.class, CharSequence.class).toString());
    }

    @Test
    public void testPrimitiveType() {
        assertEquals(setOfLong, ArchaiusType.forSetOf(long.class));
    }

    @Test
    public void testGetTypeParameters() {
        ParameterizedType archaiusType = ArchaiusType.forSetOf(Long.class);
        Type[] typeArguments = archaiusType.getActualTypeArguments();
        // check that returned array is defensively copied
        assertNotSame(typeArguments, archaiusType.getActualTypeArguments());
        assertEquals(1, typeArguments.length);
        assertEquals(Long.class, typeArguments[0]);
    }
}
