package com.netflix.archaius.test;

import static org.junit.Assert.*;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@TestPropertyOverride({"classLevelProperty=present", "cascading=type"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        assertEquals("type", config.getString("cascading"));
    }
    
    @Test
    public void testRuntimeOverrides() {
        config.setProperty("runtimeLevelProperty", "present");
        assertEquals("present", config.getRawProperty("runtimeLevelProperty"));
    }
            
    @Test
    @TestPropertyOverride({"cascading=test"})
    public void testOverridesTypeLevelProperties() {
        assertEquals("test", config.getString("cascading"));
    }
    
    @Test
    @TestPropertyOverride({"cascading=test"})
    public void testRuntimePropertyOverridesTestLevelProperties() {
        assertEquals("test", config.getString("cascading"));
        config.setProperty("cascading", "runtime");
        assertEquals("runtime", config.getString("cascading"));
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
    
    @Test
    public void zz_testPropertiesCleanedBetweenRuns() {
        assertNull(config.getRawProperty("testLevelProperty"));
        assertNull(config.getRawProperty("runtimeLevelProperty"));
        assertNull(config.getRawProperty("foo"));
    }
}
