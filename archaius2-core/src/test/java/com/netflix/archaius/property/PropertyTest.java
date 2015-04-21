/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.property;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.Property;
import com.netflix.archaius.config.SimpleDynamicConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertyTest {
    public static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        
        public MyService(DefaultAppConfig config) {
            value  = config.getProperty("foo").asInteger(1).addListener(new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.getProperty("foo").asInteger(2);
        }
        
        public void setValue(Integer value) {
            System.out.println("Updating " + value);
        }
    }
    
    @Test
    public void test() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder().withApplicationConfigName("application").build();
        
        System.out.println("Configs: " + config.getConfigNames());
        
        MyService service = new MyService(config);

        Assert.assertEquals(1, (int)service.value.get());
        Assert.assertEquals(2, (int)service.value2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, (int)service.value.get());
        Assert.assertEquals(123, (int)service.value2.get());
    }
    
    @Test
    public void testPropertyIsCached() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder().withApplicationConfigName("application").build();
        
        System.out.println("Configs: " + config.getConfigNames());
        
        Property<Integer> intProp1 = config.getProperty("foo").asInteger(1);
        Property<Integer> intProp2 = config.getProperty("foo").asInteger(2);
        Property<String>  strProp  = config.getProperty("foo").asString("3");

        Assert.assertEquals(1, (int)intProp1.get());
        Assert.assertEquals(2, (int)intProp2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get());
        Assert.assertEquals((Integer)123, intProp1.get());
        Assert.assertEquals((Integer)123, intProp2.get());
    }

    @Test
    public void testUpdateDynamicChild() throws ConfigException {
        SimpleDynamicConfig dynamic = new SimpleDynamicConfig("dyn");
        
        DefaultAppConfig config = DefaultAppConfig.builder().withApplicationConfigName("application").build();
        config.addOverrideConfig(dynamic);
        
        System.out.println("Configs: " + config.toString());
        
        Property<Integer> intProp1 = config.getProperty("foo").asInteger(1);
        Property<Integer> intProp2 = config.getProperty("foo").asInteger(2);
        Property<String>  strProp  = config.getProperty("foo").asString("3");

        Assert.assertEquals(1, (int)intProp1.get());
        Assert.assertEquals(2, (int)intProp2.get());
        
        dynamic.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get());
        Assert.assertEquals((Integer)123, intProp1.get());
        Assert.assertEquals((Integer)123, intProp2.get());
    }

}
