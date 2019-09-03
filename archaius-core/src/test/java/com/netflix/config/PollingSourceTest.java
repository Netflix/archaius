/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.event.ConfigurationErrorEvent;
import org.apache.commons.configuration.event.ConfigurationErrorListener;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class PollingSourceTest {
    
    static class DummyPollingSource implements PolledConfigurationSource {

        private boolean incremental;
        private Map<String, Object> full, added, deleted, changed;
        private Exception exception;

        public DummyPollingSource(boolean incremental) {
            this.incremental = incremental;
        }
        
        public synchronized void setIncremental(boolean value) {
            this.incremental = value;
        }
        
        public synchronized Map<String, Object> parse(String content) {
            final Map<String, Object> map = new ConcurrentHashMap<String, Object>();
            final String[] pairs = content.split(",");
            if (pairs != null) {
                for (String pair : pairs) {
                    String[] nameValue = pair.trim().split("=");
                    if (nameValue.length == 2) {
                        map.put(nameValue[0], nameValue[1]);
                    }
                }
            }
            return map;
        }
        
        public synchronized void setFull(String content) {
            full = parse(content);
        }

        public synchronized void setAdded(String content) {
            added = parse(content);
        }

        public synchronized void setDeleted(String content) {
            deleted = parse(content);
        }

        public synchronized void setChanged(String content) {
            changed = parse(content);
        }

        public synchronized void setException(Exception e) {
            this.exception = e;
        }

        @Override
        public synchronized PollResult poll(boolean initial, Object checkPoint) throws Exception {
            if (exception != null) {
                throw exception;
            } else if (incremental) {
                return PollResult.createIncremental(added, changed, deleted, null);                
            } else {
                return PollResult.createFull(full);
            }
        }
    }
    
    @Test
    public void testDeletingPollingSource() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        config.addProperty("prop1", "original");
        DummyPollingSource source = new DummyPollingSource(false);        
        source.setFull("prop1=changed");
        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 10, false);
        ConfigurationWithPollingSource pollingConfig = new ConfigurationWithPollingSource(config, source, scheduler);        
        Thread.sleep(200);
        assertEquals("changed", pollingConfig.getProperty("prop1"));
        
        source.setFull("");
        Thread.sleep(250);
        assertFalse(pollingConfig.containsKey("prop1"));
        source.setFull("prop1=changedagain,prop2=new");
        Thread.sleep(200);
        assertEquals("changedagain", pollingConfig.getProperty("prop1"));
        assertEquals("new", pollingConfig.getProperty("prop2"));
        source.setFull("prop3=new");
        Thread.sleep(200);
        assertFalse(pollingConfig.containsKey("prop1"));
        assertFalse(pollingConfig.containsKey("prop2"));
        assertEquals("new", pollingConfig.getProperty("prop3"));
    }
    
    @Test
    public void testNoneDeletingPollingSource() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        config.addProperty("prop1", "original");
        DummyPollingSource source = new DummyPollingSource(false);           
        source.setFull("");
        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 10, true);
        ConfigurationWithPollingSource pollingConfig = new ConfigurationWithPollingSource(config, source, scheduler);
        Thread.sleep(200);
        assertEquals("original", pollingConfig.getProperty("prop1"));
        source.setFull("prop1=changed");        
        Thread.sleep(200);
        assertEquals("changed", pollingConfig.getProperty("prop1"));
        source.setFull("prop1=changedagain,prop2=new");
        Thread.sleep(200);
        assertEquals("changedagain", pollingConfig.getProperty("prop1"));
        assertEquals("new", pollingConfig.getProperty("prop2"));
        source.setFull("prop3=new");
        Thread.sleep(200);
        assertEquals("changedagain", pollingConfig.getProperty("prop1"));
        assertEquals("new", pollingConfig.getProperty("prop2"));
        assertEquals("new", pollingConfig.getProperty("prop3"));
    }
    
    @Test
    public void testIncrementalPollingSource() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        DynamicPropertyFactory.initWithConfigurationSource(config);
        DynamicStringProperty prop1 = new DynamicStringProperty("prop1", null);
        DynamicStringProperty prop2 = new DynamicStringProperty("prop2", null);
        config.addProperty("prop1", "original");
        DummyPollingSource source = new DummyPollingSource(true);  
        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 10, true);
        scheduler.setIgnoreDeletesFromSource(false);
        // ConfigurationWithPollingSource pollingConfig = new ConfigurationWithPollingSource(config, source,scheduler);
        scheduler.startPolling(source, config);
        assertEquals("original", config.getProperty("prop1"));   
        assertEquals("original", prop1.get());   
        source.setAdded("prop2=new");
        Thread.sleep(200);
        assertEquals("original", config.getProperty("prop1"));        
        assertEquals("new", config.getProperty("prop2"));
        assertEquals("new", prop2.get());    
        source.setDeleted("prop1=DoesNotMatter");
        source.setChanged("prop2=changed");
        source.setAdded("");
        Thread.sleep(200);
        assertFalse(config.containsKey("prop1"));
        assertNull(prop1.get());
        assertEquals("changed", config.getProperty("prop2"));   
        assertEquals("changed", prop2.get());
    }

    @Test
    public void notificationOnSuccess() {
        // Setup
        DummyPollingSource source = new DummyPollingSource(true);
        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 200, false);
        DynamicConfiguration config = new DynamicConfiguration(source, scheduler);

        ConfigurationListener configurationListener = Mockito.mock(ConfigurationListener.class);
        ConfigurationErrorListener errorListener = Mockito.mock(ConfigurationErrorListener.class);

        ArgumentCaptor<ConfigurationEvent> eventCapture = ArgumentCaptor.forClass(ConfigurationEvent.class);
        ArgumentCaptor<ConfigurationErrorEvent> errorCapture = ArgumentCaptor.forClass(ConfigurationErrorEvent.class);

        config.addConfigurationListener(configurationListener);
        config.addErrorListener(errorListener);

        // Run
        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

        // Verify
        Mockito.verify(configurationListener, Mockito.times(2)).configurationChanged(eventCapture.capture());
        Mockito.verify(errorListener, Mockito.never()).configurationError(errorCapture.capture());

        Assert.assertEquals(DynamicConfiguration.EVENT_RELOAD, eventCapture.getAllValues().get(0).getType());
        Assert.assertTrue(eventCapture.getAllValues().get(0).isBeforeUpdate());
        Assert.assertEquals(DynamicConfiguration.EVENT_RELOAD, eventCapture.getAllValues().get(1).getType());
        Assert.assertFalse(eventCapture.getAllValues().get(1).isBeforeUpdate());
    }

    @Test
    public void notificationOnFetchFailure() {
        // Setup
        DummyPollingSource source = new DummyPollingSource(true);

        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 200, false);
        DynamicConfiguration config = new DynamicConfiguration(source, scheduler);

        ConfigurationListener configurationListener = Mockito.mock(ConfigurationListener.class);
        ConfigurationErrorListener errorListener = Mockito.mock(ConfigurationErrorListener.class);

        ArgumentCaptor<ConfigurationEvent> eventCapture = ArgumentCaptor.forClass(ConfigurationEvent.class);
        ArgumentCaptor<ConfigurationErrorEvent> errorCapture = ArgumentCaptor.forClass(ConfigurationErrorEvent.class);

        config.addConfigurationListener(configurationListener);
        config.addErrorListener(errorListener);

        // Run
        source.setException(new Exception("Failed to load"));
        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

        // Verify
        Mockito.verify(configurationListener, Mockito.times(1)).configurationChanged(eventCapture.capture());
        Mockito.verify(errorListener, Mockito.times(1)).configurationError(errorCapture.capture());

        Assert.assertEquals(DynamicConfiguration.EVENT_RELOAD, eventCapture.getAllValues().get(0).getType());
        Assert.assertTrue(eventCapture.getAllValues().get(0).isBeforeUpdate());

        Assert.assertEquals(DynamicConfiguration.EVENT_RELOAD, errorCapture.getAllValues().get(0).getType());
        Assert.assertTrue(eventCapture.getAllValues().get(0).isBeforeUpdate());
    }
}

