package com.netflix.archaius.config;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

public class InterpolationTest {
    @Test
    public void simpleStringInterpolation() {
        MapConfig config = MapConfig.builder()
                .put("foo", "${bar}")
                .put("bar", "value")
                .build();
        
        Assert.assertEquals("value", config.getString("foo"));
    }
    
    @Test
    public void nonStringInterpolation() {
        MapConfig config = MapConfig.builder()
                .put("foo", "${bar}")
                .put("bar", "123")
                .build();
        
        Assert.assertEquals(123, config.getInteger("foo").intValue());
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
        
        Assert.assertEquals("123", composite.getString("foo"));
        Assert.assertEquals("not_found", child1.getString("foo", "not_found"));
        Assert.assertEquals("not_found", child2.getString("${parent}", "not_found"));
    }
    
    @Test
    public void stringInterpolationWithDefault() {
        MapConfig config = MapConfig.builder()
                .put("bar", "${foo:default}")
                .build();
            
        Assert.assertEquals("default", config.getString("bar"));
    }
    
    @Test
    public void numericInterpolationWithDefault() {
        MapConfig config = MapConfig.builder()
                .put("bar", "${foo:-123}")
                .build();
            
        Assert.assertEquals(-123, config.getInteger("bar").intValue());
    }
}
