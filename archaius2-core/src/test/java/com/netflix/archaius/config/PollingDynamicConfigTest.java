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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.instrumentation.AccessMonitorUtil;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.junit.TestHttpServer;
import com.netflix.archaius.property.PropertiesServerHandler;
import com.netflix.archaius.readers.URLConfigReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.netflix.archaius.TestUtils.set;
import static com.netflix.archaius.TestUtils.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PollingDynamicConfigTest {

    private static final PropertiesServerHandler prop1 = new PropertiesServerHandler();
    private static final PropertiesServerHandler prop2 = new PropertiesServerHandler();

    @RegisterExtension
    static TestHttpServer HTTP_SERVER = new TestHttpServer()
            .handler("/prop1", prop1)
            .handler("/prop2", prop2);

    private final TestHttpServer server;

    PollingDynamicConfigTest(TestHttpServer server) {
        this.server = server;
    }

    @AfterEach
    public void tearDown() {
        prop1.clear();
        prop2.clear();
    }

    @Test
    @Timeout(1000)
    public void testBasicRead() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL()
                );
        
        Map<String, String> result;
        
        prop1.setProperty("a", "a_value");
        result = reader.call().getToAdd();
        assertFalse(result.isEmpty());
        assertEquals("a_value", result.get("a"));
        
        prop1.setProperty("a", "b_value");
        result = reader.call().getToAdd();
        assertFalse(result.isEmpty());
        assertEquals("b_value", result.get("a"));
    }

    @Test
    @Timeout(1000)
    public void testCombineSources() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );
        
        assertTrue(prop1.isEmpty());
        assertTrue(prop2.isEmpty());
        
        prop1.setProperty("a", "A");
        prop2.setProperty("b", "B");
        
        Map<String, String> result = reader.call().getToAdd();

        assertEquals(2, result.size());
        assertEquals("A", result.get("a"));
        assertEquals("B", result.get("b"));
    }
    
    @Test
    @Timeout(1000)
    public void testFailure() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL());
        
        prop1.setResponseCode(500);
        assertThrows(IOException.class, reader::call);
    }
    
    @Test
    @Timeout(1000)
    public void testDynamicConfig() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );

        ManualPollingStrategy strategy = new ManualPollingStrategy();
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        
        // Initialize
        //  a=A
        //  b=B
        prop1.setProperty("a", "A");
        prop2.setProperty("b", "B");
        
        strategy.fire();
        
        // Modify
        //  a=ANew
        //  b=BNew
        assertFalse(config.isEmpty());
        assertEquals("A", config.getString("a"));
        assertEquals("B", config.getString("b"));
        
        prop1.setProperty("a", "ANew");
        prop2.setProperty("b", "BNew");
        assertEquals("A", config.getString("a"));
        assertEquals("B", config.getString("b"));
        
        // Delete 1
        //  a deleted
        //  b=BNew
        strategy.fire();
        assertEquals("ANew", config.getString("a"));
        assertEquals("BNew", config.getString("b"));

        prop1.remove("a");
        prop2.setProperty("b", "BNew");
        
        strategy.fire();
        assertNull(config.getString("a", null));
        assertEquals("BNew", config.getString("b"));
    }
    
    @Test
    @Timeout(1000)
    public void testDynamicConfigFailures() throws Exception {
        URLConfigReader reader = new URLConfigReader(
                server.getServerPathURI("/prop1").toURL(),
                server.getServerPathURI("/prop2").toURL()
                );

        ManualPollingStrategy strategy = new ManualPollingStrategy();
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        
        final AtomicInteger errorCount = new AtomicInteger();
        final AtomicInteger updateCount = new AtomicInteger();
        
        config.addListener(new DefaultConfigListener() {
            @Override
            public void onConfigUpdated(Config config) {
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
        
        assertEquals("A", config.getString("a"));
        assertEquals(0, errorCount.get());
        assertEquals(1, updateCount.get());

        // Confirm failure does not modify state of Config
        prop1.setProperty("a", "ANew");
        prop1.setResponseCode(500);

        try {
            strategy.fire();
            fail("Should have thrown an exception");
        }
        catch (Exception expected) {
            
        }
        
        assertEquals(1, errorCount.get());
        assertEquals(1, updateCount.get());
        assertEquals("A", config.getString("a"));

        // Confirm state updates after failure
        prop1.setResponseCode(200);
        
        strategy.fire();
        
        assertEquals(1, errorCount.get());
        assertEquals(2, updateCount.get());
        assertEquals("ANew", config.getString("a"));
    }

    @Test
    public void testGetKeys() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo", "foo-value");
            props.put("bar", "bar-value");
            return PollingResponse.forSnapshot(props);
        };
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        Iterator<String> emptyKeys = config.getKeys();
        assertFalse(emptyKeys.hasNext());

        strategy.fire();

        Iterator<String> keys = config.getKeys();
        Set<String> keySet = new HashSet<>();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }

        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("foo"));
        assertTrue(keySet.contains("bar"));
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo", "foo-value");
            props.put("bar", "bar-value");
            return PollingResponse.forSnapshot(props);
        };
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        strategy.fire();
        Iterator<String> keys = config.getKeys();

        assertTrue(keys.hasNext());
        keys.next();
        assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo", "foo-value");
            props.put("bar", "bar-value");
            return PollingResponse.forSnapshot(props);
        };
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        strategy.fire();

        Iterable<String> keys = config.keys();
        assertEquals(2, size(keys));
        assertEquals(set("foo", "bar"), set(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo", "foo-value");
            props.put("bar", "bar-value");
            return PollingResponse.forSnapshot(props);
        };
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        strategy.fire();

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }

    @Test
    public void testInstrumentation() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo", "foo-value");
            props.put("bar", "bar-value");
            Map<String, String> propIds = new HashMap<>();
            propIds.put("foo", "1");
            propIds.put("bar", "2");
            return PollingResponse.forSnapshot(props, propIds);
        };
        AccessMonitorUtil accessMonitorUtil = spy(AccessMonitorUtil.builder().build());
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy, accessMonitorUtil);
        strategy.fire();

        assertTrue(config.instrumentationEnabled());

        config.getRawProperty("foo");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("foo", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.getRawPropertyUninstrumented("bar");
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.forEachProperty((k, v) -> {});
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("foo", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(eq(new PropertyDetails("bar", "2", "bar-value")));
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        config.forEachPropertyUninstrumented((k, v) -> {});
        verify(accessMonitorUtil, times(3)).registerUsage(any());
    }
}
