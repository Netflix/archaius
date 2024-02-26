package com.netflix.archaius;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultPropertyContainerTest {
    @Test
    public void basicTest() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        Property<String> prop = factory.getProperty("foo").asString("default");

        assertEquals("default", prop.get());
        config.setProperty("foo", "value1");
        assertEquals("value1", prop.get());
        assertEquals("value1", prop.get());
    }
}
