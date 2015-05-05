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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.PropertyListener;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertyTest {
    public static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        
        public MyService(PropertyFactory config) {
            value  = config.getProperty("foo").asInteger(1).addListener(new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.getProperty("foo").asInteger(2);
        }
        
        public void setValue(Integer value) {
            System.out.println("Updating " + value);
        }
    }
    
    @Test
    public void test() throws ConfigException {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        MyService service = new MyService(factory);

        Assert.assertEquals(1, (int)service.value.get());
        Assert.assertEquals(2, (int)service.value2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, (int)service.value.get());
        Assert.assertEquals(123, (int)service.value2.get());
    }
    
    @Test
    public void testPropertyIsCached() throws ConfigException {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> intProp1 = factory.getProperty("foo").asInteger(1);
        Property<Integer> intProp2 = factory.getProperty("foo").asInteger(2);
        Property<String>  strProp  = factory.getProperty("foo").asString("3");

        Assert.assertEquals(1, (int)intProp1.get());
        Assert.assertEquals(2, (int)intProp2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get());
        Assert.assertEquals((Integer)123, intProp1.get());
        Assert.assertEquals((Integer)123, intProp2.get());
    }

    @Test
    public void testUpdateDynamicChild() throws ConfigException {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> intProp1 = factory.getProperty("foo").asInteger(1);
        Property<Integer> intProp2 = factory.getProperty("foo").asInteger(2);
        Property<String>  strProp  = factory.getProperty("foo").asString("3");

        Assert.assertEquals(1, (int)intProp1.get());
        Assert.assertEquals(2, (int)intProp2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get());
        Assert.assertEquals((Integer)123, intProp1.get());
        Assert.assertEquals((Integer)123, intProp2.get());
    }

    @Test
    public void testDefaultNull() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(null);
        Assert.assertNull(prop.get());
    }
    
    @Test
    public void testDefault() {
        SettableConfig config = new DefaultSettableConfig();
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        Assert.assertEquals(123, prop.get().intValue());
    }
    
    @Test
    public void testUpdateValue() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("goo", "456");
        
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        config.setProperty("foo", 1);
        
        Assert.assertEquals(1, prop.get().intValue());
        
        config.clearProperty("foo");
        Assert.assertEquals(123, prop.get().intValue());
        
        config.setProperty("foo", "${goo}");
        Assert.assertEquals(456, prop.get().intValue());

    }
    
    @Test
    public void testUpdateCallback() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("goo", "456");
        
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
        
        Property<Integer> prop = factory.getProperty("foo").asInteger(123);
        final AtomicInteger current = new AtomicInteger();
        prop.addListener(new PropertyListener<Integer>() {
            @Override
            public void onChange(Integer value) {
                System.out.println("Value: " + value);
                current.set(value);
            }

            @Override
            public void onParseError(Throwable error) {
            }
        });
        
        Assert.assertEquals(123, current.intValue());
        config.setProperty("foo", 1);
        Assert.assertEquals(1, current.intValue());
        config.setProperty("foo", 2);
        Assert.assertEquals(2, current.intValue());
        config.clearProperty("foo");
        Assert.assertEquals(123, current.intValue());
        config.setProperty("foo", "${goo}");
        Assert.assertEquals(456, current.intValue());
    }
}
