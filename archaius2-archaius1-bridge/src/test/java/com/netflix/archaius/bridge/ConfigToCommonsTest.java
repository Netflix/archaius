package com.netflix.archaius.bridge;

import java.util.NoSuchElementException;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.netflix.archaius.config.MapConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigToCommonsTest {
    private final ConfigToCommonsAdapter config = new ConfigToCommonsAdapter(MapConfig.builder()
            .put("boolean", true)
            .put("string", "set")
            .put("interpolated", "${string}")
            .build()
            );
    
    @Test
    public void testIsEmptyAPI() {
        assertFalse(config.isEmpty());
    }
    
    @Test
    public void confirmStringWorks() {
        assertEquals("set", config.getString("string"));
    }
    
    @Test
    public void confirmInterpolationWorks() {
        assertEquals("set", config.getString("interpolated"));
    }
    
    @Test
    public void configNonStringWorks() {
        assertTrue(config.getBoolean("boolean"));
    }
    
    @Test
    public void configNonExistentKeyWorks() {
        assertThrows(NoSuchElementException.class, () -> config.getString("nonexistent", null));
    }
    
    @Test
    public void configIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> config.setProperty("foo", "bar"));
    }
    
    @Test
    public void test() {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.setProperty("foo", "bar,bar1");
    }
}
