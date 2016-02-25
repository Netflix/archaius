package com.netflix.config;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DynamicMapPropertyTest {
    
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
    public void testCallbacksAddUnsubscribe() {
        DynamicIntegerSetProperty dp = new DynamicIntegerSetProperty("testProperty", Sets.newTreeSet(Arrays.asList(1,2,3,4,5)));
        Set<Integer> current = dp.get();
        
        Assert.assertEquals(Arrays.asList(1,2,3,4,5), Lists.newArrayList(current));
        
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "1,2,3,4,5,6,7,8,9");
        current = dp.get();
        
        Assert.assertEquals(Arrays.asList(1,2,3,4,5,6,7,8,9), Lists.newArrayList(current));
    }
}
