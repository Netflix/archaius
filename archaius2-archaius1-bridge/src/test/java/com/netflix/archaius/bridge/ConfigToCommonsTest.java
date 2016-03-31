package com.netflix.archaius.bridge;

import java.util.NoSuchElementException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.config.MapConfig;

public class ConfigToCommonsTest {
    private ConfigToCommonsAdapter config = new ConfigToCommonsAdapter(MapConfig.builder()
            .put("boolean", true)
            .put("string", "set")
            .put("interpolated", "${string}")
            .build()
            );
    
    @Test
    public void testIsEmptyAPI() {
        Assert.assertFalse(config.isEmpty());
    }
    
    @Test
    public void confirmStringWorks() {
        Assert.assertEquals("set", config.getString("string"));
    }
    
    @Test
    public void confirmInterpolationWorks() {
        Assert.assertEquals("set", config.getString("interpolated"));
    }
    
    @Test
    public void configNonStringWorks() {
        Assert.assertEquals(true, config.getBoolean("boolean"));
    }
    
    @Test(expected=NoSuchElementException.class)
    public void configNonExistentKeyWorks() {
        Assert.assertNull(config.getString("nonexistent", null));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void configIsImmutable() {
        config.setProperty("foo", "bar");
    }
    
    @Test
    public void test() {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.setProperty("foo", "bar,bar1");
    }
}
