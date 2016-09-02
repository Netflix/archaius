package com.netflix.archaius;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;

public class DefaultPropertyContainerTest {
    @Test
    public void basicTest() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        Property<String> prop = factory.getProperty("foo").asString("default");

        Assert.assertEquals("default", prop.get());
        config.setProperty("foo", "value1");
        Assert.assertEquals("value1", prop.get());
        Assert.assertEquals("value1", prop.get());
    }
}
