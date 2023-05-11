package com.netflix.archaius.config;

import com.netflix.archaius.api.config.SettableConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Properties;

import static com.netflix.archaius.TestUtils.set;
import static com.netflix.archaius.TestUtils.size;

public class DefaultSettableConfigTest {

    @Test
    public void testGetKeys() {
        SettableConfig config = new DefaultSettableConfig();

        Assert.assertFalse(config.getKeys().hasNext());

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");
        config.setProperty("prop3", "value3");

        Assert.assertEquals(set("prop1", "prop2", "prop3"), set(config.getKeys()));

        config.clearProperty("prop3");
        Assert.assertEquals(set("prop1", "prop2"), set(config.getKeys()));

        config.setProperties(MapConfig.builder().put("prop4", "value4").build());
        Assert.assertEquals(set("prop1", "prop2", "prop4"), set(config.getKeys()));

        Properties props = new Properties();
        props.put("prop5", "value5");
        config.setProperties(props);
        Assert.assertEquals(set("prop1", "prop2", "prop4", "prop5"), set(config.getKeys()));
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() {
        SettableConfig config = new DefaultSettableConfig();

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");

        Assert.assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        config.clearProperty("prop2");

        Assert.assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        config.setProperties(MapConfig.builder().put("prop3", "value3").build());

        Assert.assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);

        Properties properties = new Properties();
        properties.put("prop5", "value5");
        config.setProperties(properties);

        Assert.assertThrows(UnsupportedOperationException.class, config.getKeys()::remove);
    }

    @Test
    public void testKeysIterable() {
        SettableConfig config = new DefaultSettableConfig();

        Assert.assertEquals(0, size(config.keys()));

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");
        config.setProperty("prop3", "value3");

        Assert.assertEquals(set("prop1", "prop2", "prop3"), set(config.keys()));

        config.clearProperty("prop3");
        Assert.assertEquals(set("prop1", "prop2"), set(config.keys()));

        config.setProperties(MapConfig.builder().put("prop4", "value4").build());
        Assert.assertEquals(set("prop1", "prop2", "prop4"), set(config.keys()));

        Properties props = new Properties();
        props.put("prop5", "value5");
        config.setProperties(props);
        Assert.assertEquals(set("prop1", "prop2", "prop4", "prop5"), set(config.keys()));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        SettableConfig config = new DefaultSettableConfig();

        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");

        Assert.assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        config.clearProperty("prop2");

        Assert.assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        config.setProperties(MapConfig.builder().put("prop3", "value3").build());

        Assert.assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);

        Properties properties = new Properties();
        properties.put("prop4", "value4");
        config.setProperties(properties);

        Assert.assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        Assert.assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }
}
