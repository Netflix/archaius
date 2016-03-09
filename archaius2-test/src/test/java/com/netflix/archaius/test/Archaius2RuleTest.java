package com.netflix.archaius.test;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

@TestPropertyOverride({"typeLevelProperty=present", "cascading=type"})
public class Archaius2RuleTest {
    
    @Rule
    public Archaius2TestConfig config = new Archaius2TestConfig();
    
    @Test 
    @TestPropertyOverride({"testLevelProperty=present"})
    public void testBasicPropertyResolution() {
        assertNotNull(config.get());
        assertEquals("present", config.get().getString("typeLevelProperty"));
        assertEquals("present", config.get().getString("testLevelProperty"));
    }
    
    @Test
    @TestPropertyOverride({"cascading=test"})
    public void testTestOverridesTypeLevelProperties() {
        assertEquals("test", config.get().getString("cascading"));
    }
    
    
    @Test
    @TestPropertyOverride({"=foo"})
    public void testEmptyKey() {
        assertEquals("foo", config.get().getString(""));
    }
    
    @Test
    @TestPropertyOverride({"foo="})
    public void testEmptyValue() {
        assertEquals("", config.get().getString("foo"));
    }
        
    @Test
    @TestPropertyOverride({"foo=bar=bad"})
    public void testMultipleDelimiter() {
        assertEquals("bar=bad", config.get().getString("foo"));
    }

}
