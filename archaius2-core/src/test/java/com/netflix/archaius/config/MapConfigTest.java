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
package com.netflix.archaius.config;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.exceptions.ParseException;

public class MapConfigTest {
    private MapConfig config = MapConfig.builder()
            .put("str", "value")
            .put("badnumber", "badnumber")
            .build();
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantString() {
        config.getString("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantBigDecimal() {
        config.getBigDecimal("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantBigInteger() {
        config.getBigInteger("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantBoolean() {
        config.getBoolean("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantByte() {
        config.getByte("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantDouble() {
        config.getDouble("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantFloat() {
        config.getFloat("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantInteger() {
        config.getInteger("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantList() {
        config.getList("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantLong() {
        config.getLong("nonexistent");
    }
    
    @Test(expected=NoSuchElementException.class)
    public void nonExistantShort() {
        config.getShort("nonexistent");
    }
    
    @Test(expected=ParseException.class)
    public void invalidBigDecimal() {
        config.getBigDecimal("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidBigInteger() {
        config.getBigInteger("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidBoolean() {
        config.getBoolean("badnumber");
    }
    
    @Test(expected=Exception.class)
    @Ignore
    public void invalidByte() {
        config.getByte("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidDouble() {
        config.getDouble("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidFloat() {
        config.getFloat("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidInteger() {
        config.getInteger("badnumber");
    }
    
    @Test(expected=Exception.class)
    @Ignore
    public void invalidList() {
        // TODO
    }
    
    @Test(expected=ParseException.class)
    public void invalidLong() {
        config.getLong("badnumber");
    }
    
    @Test(expected=ParseException.class)
    public void invalidShort() {
        config.getShort("badnumber");
    }
    
    @Test
    public void interpolationShouldWork() throws ConfigException {
        Config config = MapConfig.builder()
                .put("env",         "prod")
                .put("replacement", "${env}")
                .build();
        
        Assert.assertEquals("prod", config.getString("replacement"));
    }
    
    @Test
    public void interpolationWithDefaultReplacement() throws ConfigException {
        Config config = MapConfig.builder()
                .put("env",         "prod")
                .put("replacement", "${env}")
                .build();
        
        Assert.assertEquals("prod", config.getString("nonexistent", "${env}"));
    }
    
    @Test(expected=IllegalStateException.class)
    public void infiniteInterpolationRecursionShouldFail() throws ConfigException  {
        Config config = MapConfig.builder()
                .put("env", "${env}")
                .put("replacement.env", "${env}")
                .build();
        
        Assert.assertEquals("prod", config.getString("replacement.env"));
    }
    
    @Test
    public void numericInterpolationShouldWork() throws ConfigException  {
        Config config = MapConfig.builder()
                .put("default",     "123")
                .put("value",       "${default}")
                .build();
        
        Assert.assertEquals((long)123L, (long)config.getLong("value"));
    }
    
    @Test
    public void getListShouldWork() {
        List<String> expected = Arrays.asList("foo", "bar");
        Config config = MapConfig.builder().put("key", expected).build();
        Assert.assertEquals(expected, config.getList("key", String.class));
    }
}
