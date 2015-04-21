package com.netflix.archaius.persisted2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class FirstScopePropertyValueResolverTest {
    private final ScopePriorityPropertyValueResolver resolver = new ScopePriorityPropertyValueResolver();
    
    @Test
    public void testSingle() {
        List<ScopedValue> variations = Arrays.asList(
            create("1", "s1", "a")
        );
        
        Assert.assertEquals("1", resolver.resolve("propName", variations));
    }
    
    @Test
    public void testIdentical() {
        List<ScopedValue> variations = Arrays.asList(
            create("1", "s1", "a"),
            create("2", "s1", "a")
        );
        
        Assert.assertEquals("1", resolver.resolve("propName", variations));
    }
    
    @Test
    public void testFirst() {
        List<ScopedValue> variations = Arrays.asList(
            create("1", "s1", "a"),
            create("2", "s1", "")
        );

        Assert.assertEquals("1", resolver.resolve("propName", variations));
    }
    
    @Test
    public void testSecond() {
        List<ScopedValue> variations = Arrays.asList(
            create("1", "s1", ""),
            create("2", "s1", "a")
        );
        
        Assert.assertEquals("2", resolver.resolve("propName", variations));
    }
    
    @Test
    public void test2Scopes() {
        List<ScopedValue> variations = Arrays.asList(
            create("1", "s1", "", "s2", "",  "s3", "b"),
            create("2", "s1", "", "s2", "a,b", "s3", "")
        );
        
        Assert.assertEquals("2", resolver.resolve("propName", variations));
    }
    
    ScopedValue create(String value, String... keyValuePairs) {
        LinkedHashMap<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            Set<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(StringUtils.split(keyValuePairs[i+1], ",")));
            map.put(keyValuePairs[i], set);
        }
        return new ScopedValue(value, map);
    }
}
