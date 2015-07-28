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
package com.netflix.archaius.mapper;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.annotations.DefaultValue;
import com.netflix.archaius.annotations.PropertyName;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class ProxyFactoryTest {
    public static interface MyConfig {
        @DefaultValue("default")
        String getString();
        
        @DefaultValue("123")
        int getInteger();
        
        Integer getInteger2();
        
        @DefaultValue("true")
        boolean getBoolean();
        
        Boolean getBoolean2();
        
        @DefaultValue("3")
        short getShort();
        
        Short getShort2();
        
        @DefaultValue("3")
        long getLong();
        
        Long getLong2();

        @DefaultValue("3.1")
        float getFloat();
        
        Float getFloat2();
        
        @DefaultValue("3.1")
        double getDouble();
        
        Double getDouble2();
        
        @DefaultValue("default")
        @PropertyName(name="renamed.string")
        String getRenamed();
        
        @DefaultValue("default")
        String noVerb();
        
        @DefaultValue("false")
        boolean isIs();
    }
    
    @Test
    public void testProxy() throws ConfigException {
        Properties props = new Properties();
        props.put("prefix.string",   "loaded");
        props.put("prefix.integer",  1);
        props.put("prefix.integer2", 2);
        props.put("prefix.boolean",  true);
        props.put("prefix.boolean2", true);
        props.put("prefix.short",    1);
        props.put("prefix.short2",   2);
        props.put("prefix.long",     1);
        props.put("prefix.long2",    2);
        props.put("prefix.float",    1.1);
        props.put("prefix.float2",   2.1);
        props.put("prefix.double",   1.1);
        props.put("prefix.double2",  2.1);
        props.put("prefix.renamed.string", "loaded");
        props.put("prefix.noVerb",   "loaded");
        props.put("prefix.is",       "true");
        Config config = MapConfig.from(props);

        ConfigProxyFactory proxy = new ConfigProxyFactory(config.getDecoder(), new DefaultPropertyFactory(config.getPrefixedView("prefix")));
        MyConfig c = proxy.newProxy(MyConfig.class);

        Assert.assertEquals("loaded", c.getString());
        Assert.assertEquals("loaded", c.getRenamed());
        Assert.assertEquals("loaded", c.noVerb());
        Assert.assertEquals(1, c.getInteger());
        Assert.assertEquals(2, (int)c.getInteger2());
        Assert.assertEquals(true, c.getBoolean());
        Assert.assertEquals(true, c.getBoolean2());
        Assert.assertEquals(true, c.isIs());
        Assert.assertEquals(1, c.getShort());
        Assert.assertEquals(2, (short)c.getShort2());
        Assert.assertEquals(1, c.getLong());
        Assert.assertEquals(2, (long)c.getLong2());
        Assert.assertEquals(1.1f, c.getFloat(), 0);
        Assert.assertEquals(2.1f, (float)c.getFloat2(), 0);
        Assert.assertEquals(1.1, c.getDouble(), 0);
        Assert.assertEquals(2.1, (double)c.getDouble2(), 0);
        
        System.out.println(c.toString());
    }
    
    @Test
    public void testProxyWithDefaults() throws ConfigException{
        Config config = EmptyConfig.INSTANCE;
        
        ConfigProxyFactory proxy = new ConfigProxyFactory(config.getDecoder(), new DefaultPropertyFactory(config.getPrefixedView("prefix")));
        MyConfig c = proxy.newProxy(MyConfig.class);
        
        Assert.assertEquals(123, c.getInteger());
        try {
            Assert.assertNull(c.getInteger2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        Assert.assertEquals(true, c.getBoolean());
        try {
            Assert.assertNull(c.getBoolean2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        Assert.assertEquals(3, c.getShort());
        try {
            Assert.assertNull(c.getShort2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        Assert.assertEquals(3, c.getLong());
        try {
            Assert.assertNull(c.getLong2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        Assert.assertEquals(3.1f, c.getFloat(), 0);
        try {
            Assert.assertNull(c.getFloat2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        Assert.assertEquals(3.1, c.getDouble(), 0);
        try {
            Assert.assertNull(c.getDouble2());
            Assert.fail("Should fail on property not found");
        }
        catch (Exception e) {
            
        }
        
        System.out.println(c.toString());
    }
}
