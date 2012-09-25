package com.netflix.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class DynamicStringCollectionTest {

    @Test
    public void testStringList() {
        DynamicStringListProperty prop = new DynamicStringListProperty("test1", (String) null);
        assertNull(prop.get());
        DynamicStringListProperty prop2 = new DynamicStringListProperty("test1.2", (List<String>) null);
        assertNull(prop2.get());        
        List<String> empty = Collections.emptyList();
        prop2 = new DynamicStringListProperty("test1.2", empty);
        assertTrue(prop2.get().isEmpty());        

        ConfigurationManager.getConfigInstance().setProperty("test1", "0,1,2,3,4");
        List<String> values = prop.get();
        assertEquals(5, values.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(String.valueOf(i), values.get(i));
        }
        prop2 = new DynamicStringListProperty("test1", (List<String>) null);
        assertEquals(5, prop2.get().size());
    }
    
    @Test
    public void testListListener() {
        final List<String> result = new ArrayList<String>();
        final DynamicStringListProperty prop = new DynamicStringListProperty("test2", "0|1", "\\|") {
            protected void propertyChanged() {
                result.addAll(get());
            }
        };        
        List<String> values = prop.get();
        assertEquals(2, values.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(String.valueOf(i), prop.get().get(i));
        }
        assertTrue(result.isEmpty());
        ConfigurationManager.getConfigInstance().setProperty("test2", "0|1|2|3|4");
        assertEquals(5, prop.get().size());
        for (int i = 0; i < 5; i++) {
            assertEquals(String.valueOf(i), prop.get().get(i));
        }
        assertEquals(5, result.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(String.valueOf(i), result.get(i));
        }        
    }
    
    @Test
    public void testStringSet() {
        DynamicStringSetProperty prop = new DynamicStringSetProperty("test3", (String) null);
        assertNull(prop.get());
        prop = new DynamicStringSetProperty("test3", (Set<String>) null, "\\|");
        assertNull(prop.get());
        ConfigurationManager.getConfigInstance().setProperty("test3", "0|1|2|3|4");
        Set<String> values = prop.get();
        assertEquals(5, values.size());
        Set<String> expected = new HashSet<String>();
        for (int i = 0; i < 5; i++) {
            expected.add(String.valueOf(i));
        }
        assertEquals(expected, values);
    }
    
    @Test
    public void testSetListener() {
        final Set<String> result = new HashSet<String>();
        final DynamicStringSetProperty prop = new DynamicStringSetProperty("test4", "0,1") {
            protected void propertyChanged() {
                result.addAll(get());
            }
        };        
        Set<String> values = prop.get();
        assertEquals(2, values.size());
        assertTrue(result.isEmpty());
        ConfigurationManager.getConfigInstance().setProperty("test4", "0,1,2,3,4");
        values = prop.get();
        Set<String> expected = new HashSet<String>();
        for (int i = 0; i < 5; i++) {
            expected.add(String.valueOf(i));
        }
        assertEquals(expected, values);
        assertEquals(expected, result);
    }
    
    @Test
    public void testStringMap() {
        DynamicStringMapProperty prop = new DynamicStringMapProperty("test5", (String) null);
        assertNull(prop.getMap());
        Map<String, String> emptyMap = Collections.emptyMap();
        final Map<String, String> extMap = new HashMap<String, String>();
        prop = new DynamicStringMapProperty("test5", emptyMap) {
            protected void propertyChanged() {
                extMap.putAll(getMap());
            }
        };
        assertTrue(prop.getMap().isEmpty());
        assertTrue(extMap.isEmpty());
        ConfigurationManager.getConfigInstance().setProperty("test5", "key1=1,key2=2,key3=3");
        Map<String, String> map = prop.getMap();
        assertEquals(3, map.size());
        for (int i = 1; i <= 3; i++) {
            assertEquals(String.valueOf(i), map.get("key" + i));
        }
        assertEquals(extMap, map);
    }
}
