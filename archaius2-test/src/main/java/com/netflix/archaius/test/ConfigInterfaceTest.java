package com.netflix.archaius.test;

import com.netflix.archaius.api.Config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public abstract class ConfigInterfaceTest {

    protected abstract Config getInstance(Map<String, String> properties);

    private final Map<String, String> props = new HashMap<>();

    public ConfigInterfaceTest() {
        props.put("goo", "baz");
    }

    @Test
    public final void getValue() throws Exception {
        Config instance = getInstance(props);
        String result = instance.getString("goo");
        assertEquals("baz", result);
    }

    @Test
    public final void getValueWithDefault() throws Exception {
        Config instance = getInstance(props);
        String result = instance.getString("foo", "bar");
        assertEquals("bar", result);
    }
}
