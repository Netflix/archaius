package com.netflix.archaius.visitor;

import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertyOverrideVisitorTest {
    @Test
    public void testNotFound() throws ConfigException {
        CompositeConfig config = CompositeConfig.create();
        
        LinkedHashMap<String, String> sources = config.accept(new PropertyOverrideVisitor("foo"));
        
        Assert.assertNull(sources);
        
    }
    
    @Test
    public void testMultiple() throws ConfigException {
        CompositeConfig config = CompositeConfig.builder().build();
        config.addConfig("a", MapConfig.builder().put("foo", "a_foo").build());
        config.addConfig("b", MapConfig.builder().put("foo", "b_foo").build());
        
        config.addConfig("gap", MapConfig.builder().put("bar", "b_bar").build());

        config.addConfig("c", CompositeConfig.builder().withConfig("d", MapConfig.builder().put("foo", "d_foo").build()).build());
        
        LinkedHashMap<String, String> sources = config.accept(new PropertyOverrideVisitor("foo"));
        
        Assert.assertEquals("a_foo", config.getString("foo"));

        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "a_foo");
        expected.put("b", "b_foo");
        expected.put("c:d", "d_foo");
        Assert.assertEquals(expected, sources);
    }
}
