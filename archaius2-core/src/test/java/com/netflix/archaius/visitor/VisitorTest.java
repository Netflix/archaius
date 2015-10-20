package com.netflix.archaius.visitor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class VisitorTest {
    @Test
    public void testNotFound() throws ConfigException {
        CompositeConfig config = CompositeConfig.create();
        
        LinkedHashMap<String, String> sources = config.accept(new PropertyOverrideVisitor("foo"));
        
        Assert.assertNull(sources);
        
    }
    
    @Test
    public void testOverrideVisitor() throws ConfigException {
        CompositeConfig config = createComposite();
        
        LinkedHashMap<String, String> sources = config.accept(new PropertyOverrideVisitor("foo"));
        
        Assert.assertEquals("a_foo", config.getString("foo"));

        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "a_foo");
        expected.put("b", "b_foo");
        expected.put("c/d", "d_foo");
        Assert.assertEquals(expected, sources);
        
        System.out.println(expected);
    }
    
    @Test
    public void testFlattenedNames() throws ConfigException {
        CompositeConfig config = createComposite();
        List<String> result = config.accept(new FlattenedNamesVisitor());
        Assert.assertEquals(Arrays.asList("a", "b", "gap", "c", "d"), result);
    }
    
    /*
     * Root 
     *   - a
     *      - foo : a_foo
     *      - foo : b_foo
     *   - b
     *   - gap
     *      - bar : b_bar
     *   - c
     *      - d
     *          - foo : d_foo
     */
    CompositeConfig createComposite() throws ConfigException {
        CompositeConfig config = CompositeConfig.create();
        
        config.addConfig("a", MapConfig.builder().put("foo", "a_foo").build());
        config.addConfig("b", MapConfig.builder().put("foo", "b_foo").build());
        
        config.addConfig("gap", MapConfig.builder().put("bar", "b_bar").build());

        config.addConfig("c", CompositeConfig.builder().withConfig("d", MapConfig.builder().put("foo", "d_foo").build()).build());
        
        LinkedHashMap<String, String> sources = config.accept(new PropertyOverrideVisitor("foo"));
        
        
        return config;
    }
}
