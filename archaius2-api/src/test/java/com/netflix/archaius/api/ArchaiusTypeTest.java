package com.netflix.archaius.api;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArchaiusTypeTest {
    @Test
    public void testEquals() {
        ParameterizedType archaiusType = ArchaiusType.forListOf(String.class);
        Assert.assertEquals(archaiusType, listOfString);
        Assert.assertEquals(listOfString, archaiusType);
        Assert.assertEquals(archaiusType, ArchaiusType.forListOf(String.class));
        Assert.assertNotEquals(archaiusType, ArchaiusType.forListOf(Integer.class));
        Assert.assertNotEquals(archaiusType, setOfLong);
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(listOfString.hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        Assert.assertEquals(ArchaiusType.forListOf(String.class).hashCode(), ArchaiusType.forListOf(String.class).hashCode());
        Assert.assertEquals(setOfLong.hashCode(), ArchaiusType.forSetOf(Long.class).hashCode());
        Assert.assertEquals(ArchaiusType.forMapOf(Integer.class, CharSequence.class).hashCode(), mapOfIntToCharSequence.hashCode());
    }

    @Test
    public void testToString() {
        Assert.assertEquals("java.util.List<java.lang.String>", ArchaiusType.forListOf(String.class).toString());
        Assert.assertEquals(listOfString.toString(), ArchaiusType.forListOf(String.class).toString());
        Assert.assertEquals(setOfLong.toString(), ArchaiusType.forSetOf(Long.class).toString());
        Assert.assertEquals(mapOfIntToCharSequence.toString(), ArchaiusType.forMapOf(Integer.class, CharSequence.class).toString());
    }

    @Test
    public void testPrimitiveType() {
        Assert.assertEquals(setOfLong, ArchaiusType.forSetOf(long.class));
    }

    @Test
    public void testGetTypeParameters() {
        ParameterizedType archaiusType = ArchaiusType.forSetOf(Long.class);
        Type[] typeArguments = archaiusType.getActualTypeArguments();
        // check that returned array is defensively copied
        Assert.assertNotSame(typeArguments, archaiusType.getActualTypeArguments());
        Assert.assertEquals(1, typeArguments.length);
        Assert.assertEquals(Long.class, typeArguments[0]);
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
