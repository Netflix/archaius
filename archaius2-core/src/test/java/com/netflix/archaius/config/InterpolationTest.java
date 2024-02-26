package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InterpolationTest {
    @Test
    public void simpleStringInterpolation() {
        MapConfig config = MapConfig.builder()
                .put("foo", "${bar}")
                .put("bar", "value")
                .build();
        
        assertEquals("value", config.getString("foo"));
    }
    
    @Test
    public void nonStringInterpolation() {
        MapConfig config = MapConfig.builder()
                .put("foo", "${bar}")
                .put("bar", "123")
                .build();
        
        assertEquals(123, config.getInteger("foo").intValue());
    }
    
    @Test
    public void interpolationOnlyDoneOnParent() throws ConfigException {
        MapConfig child1 = MapConfig.builder()
                .put("bar", "123")
                .build();
            
        MapConfig child2 = MapConfig.builder()
                .put("foo", "${bar}")
                .build();
            
        CompositeConfig composite = DefaultCompositeConfig.builder()
                .withConfig("a", child1)
                .withConfig("b", child2)
                .build();
        
        assertEquals("123", composite.getString("foo"));
        assertEquals("not_found", child1.getString("foo", "not_found"));
        assertEquals("not_found", child2.getString("${parent}", "not_found"));
    }
    
    @Test
    public void stringInterpolationWithDefault() {
        MapConfig config = MapConfig.builder()
                .put("bar", "${foo:default}")
                .build();
            
        assertEquals("default", config.getString("bar"));
    }
    
    @Test
    public void numericInterpolationWithDefault() {
        MapConfig config = MapConfig.builder()
                .put("bar", "${foo:-123}")
                .build();
            
        assertEquals(-123, config.getInteger("bar").intValue());
    }
    
    
    @Test
    public void interpolatePrefixedView() {
        Config config = MapConfig.builder()
            .put("prefix.key1",    "${nonprefixed.key2}")
            .put("nonprefixed.key2", "key2_value")
            .build();
        
        assertEquals("key2_value", config.getString("prefix.key1"));
        
        Config prefixedConfig = config.getPrefixedView("prefix");
        prefixedConfig.accept(new PrintStreamVisitor());
        assertEquals("key2_value", prefixedConfig.getString("key1"));
    }
    
    @Test
    public void interpolateNumericPrefixedView() {
        Config config = MapConfig.builder()
            .put("prefix.key1",    "${nonprefixed.key2}")
            .put("nonprefixed.key2", "123")
            .build();
        
        assertEquals(123, config.getInteger("prefix.key1").intValue());
        
        Config prefixedConfig = config.getPrefixedView("prefix");
        
        assertEquals(123, prefixedConfig.getInteger("key1").intValue());
    }

    @Test
    public void nestedInterpolations() {
        Config config = MapConfig.builder()
                .put("a", "A")
                .put("b", "${c:${a}}")
                .build();

        assertEquals("A", config.getString("b"));
    }

    @Test
    public void nestedInterpolationsWithResolve() {
        Config config = MapConfig.builder()
                .put("a", "A")
                .build();

        assertEquals("A", config.resolve("${b:${c:${a}}}"));
    }

    @Test
    public void failOnCircularResolve() {
        Config config = MapConfig.builder()
                .build();
        assertThrows(IllegalStateException.class, () -> config.resolve("${b:${c:${b}}}"));
    }
    
    @Test
    public void returnStringOnMissingInterpolation() {
        Config config = MapConfig.builder()
                .build();

        assertEquals("${c}", config.resolve("${b:${c}}"));
        
    }
}
