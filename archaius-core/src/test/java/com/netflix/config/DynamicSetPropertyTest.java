package com.netflix.config;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DynamicSetPropertyTest {
    
    public class DynamicIntegerSetProperty extends DynamicSetProperty<Integer> {
        public DynamicIntegerSetProperty(String propName, Set<Integer> defaultValue) {
            super(propName, defaultValue);
        }

        @Override
        protected Integer from(String value) {
            return Integer.parseInt(value);
        }
    }
    

    @Test
    public void test() {
        DynamicIntegerSetProperty dp = new DynamicIntegerSetProperty("testProperty", Sets.newLinkedHashSet(Arrays.asList(5,1,2,3,4)));
        Set<Integer> current = dp.get();
        
        Assert.assertEquals(Arrays.asList(5,1,2,3,4), Lists.newArrayList(current));
        
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "2,3,4,5,6,7,8,9,1");
        current = dp.get();
        
        Assert.assertEquals(Arrays.asList(2,3,4,5,6,7,8,9,1), Lists.newArrayList(current));
    }
}
