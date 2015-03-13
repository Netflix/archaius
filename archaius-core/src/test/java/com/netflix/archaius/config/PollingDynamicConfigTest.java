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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.netflix.archaius.Config;
import com.netflix.archaius.DynamicConfigObserver;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.junit.TestHttpServer;
import com.netflix.archaius.property.PropertiesServerHandler;
import com.netflix.archaius.readers.URLConfigReader;

public class PollingDynamicConfigTest {
    
    private PropertiesServerHandler prop1 = new PropertiesServerHandler();
    private PropertiesServerHandler prop2 = new PropertiesServerHandler();
    
    @Rule
    public TestHttpServer server = new TestHttpServer()
            .handler("/prop1", prop1)
            .handler("/prop2", prop2)
            ;
    
    @Test(timeout=1000)
    public void testBasicRead() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL()
                );
        
        Map<String, String> result;
        
        prop1.setProperty("a", "a_value");
        result = reader.call();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals("a_value", result.get("a"));
        
        prop1.setProperty("a", "b_value");
        result = reader.call();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals("b_value", result.get("a"));
    }

    @Test(timeout=1000)
    public void testCombineSources() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );
        
        Assert.assertTrue(prop1.isEmpty());
        Assert.assertTrue(prop2.isEmpty());
        
        prop1.setProperty("a", "A");
        prop2.setProperty("b", "B");
        
        Map<String, String> result = reader.call();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("A", result.get("a"));
        Assert.assertEquals("B", result.get("b"));
    }
    
    @Test(timeout=1000, expected=IOException.class)
    public void testFailure() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL());
        
        prop1.setResponseCode(500);
        reader.call();
        
        Assert.fail("Should have failed with 500 error");
    }
    
    @Test(timeout=1000)
    public void testDynamicConfig() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );

        ManualPollingStrategy strategy = new ManualPollingStrategy();
        PollingDynamicConfig config = new PollingDynamicConfig(null, reader, strategy);
        
        // Initialize
        //  a=A
        //  b=B
        prop1.setProperty("a", "A");
        prop2.setProperty("b", "B");
        
        strategy.fire();
        
        // Modify
        //  a=ANew
        //  b=BNew
        Assert.assertFalse(config.isEmpty());
        Assert.assertEquals("A", config.getString("a"));
        Assert.assertEquals("B", config.getString("b"));
        
        prop1.setProperty("a", "ANew");
        prop2.setProperty("b", "BNew");
        Assert.assertEquals("A", config.getString("a"));
        Assert.assertEquals("B", config.getString("b"));
        
        // Delete 1
        //  a deleted
        //  b=BNew
        strategy.fire();
        Assert.assertEquals("ANew", config.getString("a"));
        Assert.assertEquals("BNew", config.getString("b"));

        prop1.remove("a");
        prop2.setProperty("b", "BNew");
        
        strategy.fire();
        Assert.assertNull(config.getString("a", null));
        Assert.assertEquals("BNew", config.getString("b"));
    }
    
    @Test(timeout=1000)
    public void testDynamicConfigFailures() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );

        ManualPollingStrategy strategy = new ManualPollingStrategy();
        PollingDynamicConfig config = new PollingDynamicConfig(null, reader, strategy);
        
        final AtomicInteger errorCount = new AtomicInteger();
        final AtomicInteger updateCount = new AtomicInteger();
        
        config.addListener(new DynamicConfigObserver() {
            @Override
            public void onUpdate(String propName, Config config) {
            }

            @Override
            public void onUpdate(Config config) {
                updateCount.incrementAndGet();
            }

            @Override
            public void onError(Throwable error, Config config) {
                errorCount.incrementAndGet();
            }
        });
        
        // Confirm success on first pass
        prop1.setProperty("a", "A");
        
        strategy.fire();
        
        Assert.assertEquals("A", config.getString("a"));
        Assert.assertEquals(0, errorCount.get());
        Assert.assertEquals(1, updateCount.get());

        // Confirm failure does not modify state of Config
        prop1.setProperty("a", "ANew");
        prop1.setResponseCode(500);

        strategy.fire();
        
        Assert.assertEquals(1, errorCount.get());
        Assert.assertEquals(1, updateCount.get());
        Assert.assertEquals("A", config.getString("a"));

        // Confim state updates after failure
        prop1.setResponseCode(200);
        
        strategy.fire();
        
        Assert.assertEquals(1, errorCount.get());
        Assert.assertEquals(2, updateCount.get());
        Assert.assertEquals("ANew", config.getString("a"));
    }
}
