package com.netflix.archaius.api;

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
    @Test
    public void testEquals() {
        ParameterizedType archaiusType = ArchaiusType.forListOf(String.class);
        assertEquals(archaiusType, listOfString);
        assertEquals(listOfString, archaiusType);
        assertEquals(archaiusType, ArchaiusType.forListOf(String.class));
        assertNotEquals(archaiusType, ArchaiusType.forListOf(Integer.class));
        assertNotEquals(archaiusType, setOfLong);
    }

    @Test
    public void testHashCode() {
        assertEquals(listOfString.hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        assertEquals(ArchaiusType.forListOf(String.class).hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        assertEquals(setOfLong.hashCode(), ArchaiusType.forSetOf(Long.class).hashCode());
        assertEquals(ArchaiusType.forMapOf(Integer.class, CharSequence.class).hashCode(), mapOfIntToCharSequence.hashCode());
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

    private static List<String> listOfString() { throw new AssertionError(); }
    private static Set<Long> setOfLong() { throw new AssertionError(); }
    private static Map<Integer, CharSequence> mapOfIntToCharSequence() { throw new AssertionError(); }
    private static final ParameterizedType listOfString;
    private static final ParameterizedType setOfLong;
    private static final ParameterizedType mapOfIntToCharSequence;

    static {
        try {
            listOfString = (ParameterizedType) ArchaiusTypeTest.class.getDeclaredMethod("listOfString").getGenericReturnType();
            setOfLong = (ParameterizedType) ArchaiusTypeTest.class.getDeclaredMethod("setOfLong").getGenericReturnType();
            mapOfIntToCharSequence = (ParameterizedType) ArchaiusTypeTest.class.getDeclaredMethod("mapOfIntToCharSequence").getGenericReturnType();
        } catch (NoSuchMethodException exc) {
            throw new AssertionError("Method not found", exc);
        }
    }
}
