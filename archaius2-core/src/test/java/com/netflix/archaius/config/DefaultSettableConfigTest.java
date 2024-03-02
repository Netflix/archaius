package com.netflix.archaius.config;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.netflix.archaius.api.config.SettableConfig;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultSettableConfigTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testGetKeys() {
        SettableConfig config = new DefaultSettableConfig();

        assertFalse(config.getKeys().hasNext());

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");
        config.setProperty("prop3", "value3");

        assertEquals(Sets.newHashSet("prop1", "prop2", "prop3"), Sets.newHashSet(config.getKeys()));

        config.clearProperty("prop3");
        assertEquals(Sets.newHashSet("prop1", "prop2"), Sets.newHashSet(config.getKeys()));

        config.setProperties(MapConfig.builder().put("prop4", "value4").build());
        assertEquals(Sets.newHashSet("prop1", "prop2", "prop4"), Sets.newHashSet(config.getKeys()));

        Properties props = new Properties();
        props.put("prop5", "value5");
        config.setProperties(props);
        assertEquals(Sets.newHashSet("prop1", "prop2", "prop4", "prop5"), Sets.newHashSet(config.getKeys()));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetKeysIteratorRemoveThrows() {
        SettableConfig config = new DefaultSettableConfig();

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");

        assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        config.clearProperty("prop2");

        assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        config.setProperties(MapConfig.builder().put("prop3", "value3").build());

        assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        Properties properties = new Properties();
        properties.put("prop5", "value5");
        config.setProperties(properties);

        assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);
    }

    @Test
    public void testKeysIterable() {
        SettableConfig config = new DefaultSettableConfig();

        assertEquals(0, Iterables.size(config.keys()));

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");
        config.setProperty("prop3", "value3");

        assertEquals(Sets.newHashSet("prop1", "prop2", "prop3"), Sets.newHashSet(config.keys()));

        config.clearProperty("prop3");
        assertEquals(Sets.newHashSet("prop1", "prop2"), Sets.newHashSet(config.keys()));

        config.setProperties(MapConfig.builder().put("prop4", "value4").build());
        assertEquals(Sets.newHashSet("prop1", "prop2", "prop4"), Sets.newHashSet(config.keys()));

        Properties props = new Properties();
        props.put("prop5", "value5");
        config.setProperties(props);
        assertEquals(Sets.newHashSet("prop1", "prop2", "prop4", "prop5"), Sets.newHashSet(config.keys()));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        SettableConfig config = new DefaultSettableConfig();

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        config.clearProperty("prop2");

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        config.setProperties(MapConfig.builder().put("prop3", "value3").build());

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        Properties properties = new Properties();
        properties.put("prop4", "value4");
        config.setProperties(properties);

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }
}
