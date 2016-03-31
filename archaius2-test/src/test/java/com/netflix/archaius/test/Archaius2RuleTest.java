package com.netflix.archaius.test;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

@TestPropertyOverride({"classLevelProperty=present", "cascading=type"})
public class Archaius2RuleTest extends Archaius2RuleTestParentTest {
    
    @Rule
    public Archaius2TestConfig config = new Archaius2TestConfig();
    
    @Test 
    @TestPropertyOverride({"testLevelProperty=present"})
    public void testBasicPropertyResolution() {
        assertNotNull(config);
        assertEquals("present", config.getString("parentClassLevelProperty"));
        assertEquals("present", config.getString("classLevelProperty"));
        assertEquals("present", config.getString("testLevelProperty"));
    }
            
    @Test
    @TestPropertyOverride({"cascading=test"})
    public void testTestOverridesTypeLevelProperties() {
        assertEquals("test", config.getString("cascading"));
    }
    
    
    @Test
    @TestPropertyOverride({"=foo"})
    public void testEmptyKey() {
        assertEquals("foo", config.getString(""));
    }
    
    @Test
    @TestPropertyOverride({"foo="})
    public void testEmptyValue() {
        assertEquals("", config.getString("foo"));
    }
        
    @Test
    @TestPropertyOverride({"foo=bar=bad"})
    public void testMultipleDelimiter() {
        assertEquals("bar=bad", config.getString("foo"));
    }
    
    @Test
    @TestPropertyOverride(propertyFiles={"archaiusRuleTest.properties"})
    public void testPropertyFromFile() {
        assertEquals("present", config.getString("fileProperty"));
    }

}
