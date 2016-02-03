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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.Properties;

import org.junit.Test;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

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

        assertThat(c.getString(),        equalTo("loaded"));
        assertThat(c.getString(),        equalTo("loaded"));
        assertThat(c.getRenamed(),       equalTo("loaded"));
        assertThat(c.noVerb(),           equalTo("loaded"));
        assertThat(c.getInteger(),       equalTo(1));
        assertThat(c.getInteger2(),      equalTo(2));
        assertThat(c.getBoolean(),       equalTo(true));
        assertThat(c.getBoolean2(),      equalTo(true));
        assertThat(c.isIs(),             equalTo(true));
        assertThat((int)c.getShort(),    equalTo(1));
        assertThat((int)c.getShort2(),   equalTo(2));
        assertThat((long)c.getLong(),    equalTo(1L));
        assertThat(c.getLong2(),         equalTo(2L));
        assertThat(c.getFloat(),         equalTo(1.1f));
        assertThat(c.getFloat2(),        equalTo(2.1f));
        assertThat(c.getDouble(),        equalTo(1.1));
        assertThat(c.getDouble2(),       equalTo(2.1));
        
        System.out.println(c.toString());
    }
    
    @Test
    public void testProxyWithDefaults() throws ConfigException{
        Config config = EmptyConfig.INSTANCE;
        
        ConfigProxyFactory proxy = new ConfigProxyFactory(config.getDecoder(), new DefaultPropertyFactory(config.getPrefixedView("prefix")));
        MyConfig c = proxy.newProxy(MyConfig.class);
        
        assertThat(c.getInteger(),      equalTo(123));
        assertThat(c.getInteger2(),     nullValue());
        
        assertThat(c.getBoolean(),      equalTo(true));
        assertThat(c.getBoolean2(),     nullValue());
        
        assertThat((int)c.getShort(),   equalTo(3));
        assertThat(c.getShort2(),       nullValue());
        
        assertThat(c.getLong(),         equalTo(3L));
        assertThat(c.getLong2(),        nullValue());
        
        assertThat(c.getFloat(),        equalTo(3.1f));
        assertThat(c.getFloat2(),       nullValue());
        
        assertThat(c.getDouble(),       equalTo(3.1));
        assertThat(c.getDouble2(),      nullValue());
        
        System.out.println(c.toString());
    }
}
