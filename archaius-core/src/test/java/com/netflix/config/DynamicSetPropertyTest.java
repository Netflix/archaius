package com.netflix.config;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DynamicSetPropertyTest {
    
    public class DynamicIntegerMapProperty extends DynamicMapProperty<Integer, String> {
        public DynamicIntegerMapProperty(String propName, Map<Integer, String> defaultValue) {
            super(propName, defaultValue);
        }

        @Override
        protected String getValue(String key) {
            return key;
        }

        @Override
        protected Integer getKey(String value) {
            return Integer.parseInt(value);
        }
    }
    

    @Test
    public void testCallbacksAddUnsubscribe() {
        Map<Integer, String> map1 = new TreeMap<Integer, String>();
        map1.put(1, "a");
        map1.put(2, "b");
        map1.put(3, "c");
        map1.put(4, "d");
        
        DynamicIntegerMapProperty dp = new DynamicIntegerMapProperty("testProperty", map1);
        
        Map<Integer, String> current = dp.getMap();
        
        Assert.assertEquals(Arrays.asList(1,2,3,4,5), Lists.newArrayList(map1.keySet()));
        
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "1=a,2=a,3=a,4=a,5=a,6=a,7=a,8=a,9=a");
        current = dp.getMap();
        
        Assert.assertEquals(Arrays.asList(1,2,3,4,5,6,7,8,9), Lists.newArrayList(map1.keySet()));
    }
}
