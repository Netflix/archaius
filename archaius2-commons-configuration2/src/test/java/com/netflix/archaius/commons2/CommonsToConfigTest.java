package com.netflix.archaius.commons2;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netflix.archaius.api.ArchaiusType;
import com.netflix.archaius.config.AbstractConfig;
import org.apache.commons.configuration2.BaseConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsToConfigTest {

    private final AbstractConfig config;

    CommonsToConfigTest() {
        final BaseConfiguration baseConfiguration = new BaseConfiguration();
        baseConfiguration.addProperty("foo", "bar");
        baseConfiguration.addProperty("byte", (byte) 42);
        baseConfiguration.addProperty("int", 42);
        baseConfiguration.addProperty("long", 42L);
        baseConfiguration.addProperty("float", 42.0f);
        baseConfiguration.addProperty("double", 42.0d);
        baseConfiguration.addProperty("numberList", Arrays.asList("1", "2", "3 "));
        baseConfiguration.addProperty("stringList", new String[] {"a", "b" ,"c"});
        baseConfiguration.addProperty("uriList", new String[]{"http://example.com","http://example.org"});
        baseConfiguration.addProperty("underlyingList", Arrays.asList("a", "b", "c"));
        baseConfiguration.addProperty("springYmlList[0]", "1");
        baseConfiguration.addProperty("springYmlList[1]", "2");
        baseConfiguration.addProperty("springYmlList[2]", "3");
        baseConfiguration.addProperty("springYmlIntList[0]", 1);
        baseConfiguration.addProperty("springYmlIntList[1]", 2);
        baseConfiguration.addProperty("springYmlIntList[2]", 3);
        // Repeated entry to distinguish set and list
        baseConfiguration.addProperty("springYmlList[3]", "3");
        baseConfiguration.addProperty("springYmlMap.key1", "1");
        baseConfiguration.addProperty("springYmlMap.key2", "2");
        baseConfiguration.addProperty("springYmlMap.key3", "3");
        baseConfiguration.addProperty("springYmlWithSomeInvalidList[0]", "abc,def");
        baseConfiguration.addProperty("springYmlWithSomeInvalidList[1]", "abc");
        baseConfiguration.addProperty("springYmlWithSomeInvalidList[2]", "a=b");
        baseConfiguration.addProperty("springYmlWithSomeInvalidMap.key1", "a=b");
        baseConfiguration.addProperty("springYmlWithSomeInvalidMap.key2", "c");
        baseConfiguration.addProperty("springYmlWithSomeInvalidMap.key3", "d,e");
        config = new CommonsToConfig(baseConfiguration);
    }

    @Test
    void testGet() {
        assertEquals("bar", config.get(String.class, "foo"));
    }

    @Test
    void getExistingProperty() {
        //noinspection OptionalGetWithoutIsPresent
        assertEquals("bar", config.getProperty("foo").get());
    }

    @Test
    void getNonExistentProperty() {
        assertFalse(config.getProperty("non_existent").isPresent());
    }

    @Test
    void testGetLists() {
        assertEquals(Arrays.asList(1, 2, 3), config.getList("numberList", Integer.class));
        assertEquals(Arrays.asList(1L, 2L, 3L), config.getList("numberList", Long.class));
        // Watch out for the trailing space in the original value in the config!
        assertEquals(Arrays.asList("1", "2", "3 "), config.getList("numberList", String.class));
        assertEquals(Arrays.asList("a", "b", "c"), config.getList("stringList", String.class));
        assertEquals(Arrays.asList(URI.create("http://example.com"), URI.create("http://example.org")), config.getList("uriList", URI.class));

        // Watch out for the trailing space in the list in the original value in the config!
        assertEquals(Arrays.asList("1", "2", "3 "), config.getList("numberList"));
        assertEquals(Arrays.asList("a", "b", "c"), config.getList("stringList"));
        assertEquals(Arrays.asList("http://example.com", "http://example.org"), config.getList("uriList"));

        assertEquals(Arrays.asList("a", "b", "c"), config.getList("underlyingList"));
    }

    @SuppressWarnings("java:S5961") // Suppress SonarQube issue of too many assertions
    @Test
    void testGetRawNumerics() {
        // First, get each entry as its expected type and the corresponding wrapper.
        assertEquals(42, config.get(int.class, "int"));
        assertEquals(42, config.get(Integer.class, "int"));
        assertEquals(42L, config.get(long.class, "long"));
        assertEquals(42L, config.get(Long.class, "long"));
        assertEquals((byte) 42, config.get(byte.class, "byte"));
        assertEquals((byte) 42, config.get(Byte.class, "byte"));
        assertEquals(42.0f, config.get(float.class, "float"));
        assertEquals(42.0f, config.get(Float.class, "float"));
        assertEquals(42.0d, config.get(double.class, "double"));
        assertEquals(42.0d, config.get(Double.class, "double"));

        // Then, get each entry as a string
        assertEquals("42", config.get(String.class, "int"));
        assertEquals("42", config.get(String.class, "long"));
        assertEquals("42", config.get(String.class, "byte"));
        assertEquals("42.0", config.get(String.class, "float"));
        assertEquals("42.0", config.get(String.class, "double"));

        // Then, narrowed types
        assertEquals((byte) 42, config.get(byte.class, "int"));
        assertEquals((byte) 42, config.get(byte.class, "long"));
        assertEquals(42.0f, config.get(double.class, "double"));

        // Then, widened
        assertEquals(42L, config.get(long.class, "int"));
        assertEquals(42L, config.get(long.class, "byte"));
        assertEquals(42.0d, config.get(double.class, "float"));

        // On floating point
        assertEquals(42.0f, config.get(float.class, "int"));
        assertEquals(42.0f, config.get(float.class, "byte"));
        assertEquals(42.0f, config.get(float.class, "long"));
        assertEquals(42.0f, config.get(float.class, "double"));

        // As doubles
        assertEquals(42.0d, config.get(double.class, "int"));
        assertEquals(42.0d, config.get(double.class, "byte"));
        assertEquals(42.0d, config.get(double.class, "long"));
        assertEquals(42.0d, config.get(double.class, "float"));

        // Narrowed types in wrapper classes
        assertEquals((byte) 42, config.get(Byte.class, "int"));
        assertEquals((byte) 42, config.get(Byte.class, "long"));

        // Widened types in wrappers
        assertEquals(42L, config.get(Long.class, "int"));
        assertEquals(42L, config.get(Long.class, "byte"));
    }

    @Test
    void testSpringYml() {
        // Working cases for set, list, and map
        Set<Integer> set =
                config.get(ArchaiusType.forSetOf(Integer.class), "springYmlList", Collections.singleton(1));
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));

        List<Integer> list =
                config.get(ArchaiusType.forListOf(Integer.class), "springYmlList", Arrays.asList(1));
        assertEquals(Arrays.asList(1, 2, 3, 3), list);

        List<Integer> intList =
                config.get(ArchaiusType.forListOf(Integer.class), "springYmlIntList", Arrays.asList(1));
        assertEquals(Arrays.asList(1, 2, 3), intList);

        Map<String, Integer> map =
                config.get(ArchaiusType.forMapOf(String.class, Integer.class),
                        "springYmlMap", Collections.emptyMap());
        assertEquals(3, map.size());
        assertEquals(1, map.get("key1"));
        assertEquals(2, map.get("key2"));
        assertEquals(3, map.get("key3"));

        // Not a proper list, so we have the default value returned
        List<Integer> invalidList =
                config.get(ArchaiusType.forListOf(Integer.class), "springYmlMap", Arrays.asList(1));
        assertEquals(Collections.singletonList(1), invalidList);

        // Not a proper set, so we have the default value returned
        Set<Integer> invalidSet =
                config.get(ArchaiusType.forSetOf(Integer.class), "springYmlMap", Collections.singleton(1));
        assertEquals(Collections.singleton(1), invalidSet);

        // Not a proper map, so we have the default value returned
        Map<String, String> invalidMap =
                config.get(
                        ArchaiusType.forMapOf(String.class, String.class),
                        "springYmlList",
                        Collections.singletonMap("default", "default"));
        assertEquals(1, invalidMap.size());
        assertEquals("default", invalidMap.get("default"));
    }

    @Test
    void testSpringYamlAsNormalValue() {
        // Confirm that values that are intended to be read as a Spring YML Map can still be read normally
        // and also do not return values when read at the top level as anything other than a map.
        assertEquals("1", config.get(String.class, "springYmlMap.key1"));
        assertEquals(2, config.get(Integer.class, "springYmlMap.key2"));
        assertFalse(config.containsKey("springYmlMap"));
    }
}