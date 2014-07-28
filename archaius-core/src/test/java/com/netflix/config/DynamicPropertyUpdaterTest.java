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

import com.google.common.collect.Maps;
import com.netflix.config.AbstractDynamicPropertyListener;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ExpandedConfigurationListenerAdapter;
import com.netflix.config.WatchedUpdateResult;
import com.netflix.config.AbstractDynamicPropertyListener.EventType;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;


public class DynamicPropertyUpdaterTest {

    DynamicPropertyUpdater dynamicPropertyUpdater;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        dynamicPropertyUpdater = new DynamicPropertyUpdater();
    }

    /**
     * Test method for {@link com.charter.aesd.archaius.DynamicPropertyUpdater#updateProperties(com.netflix.config.WatchedUpdateResult, org.apache.commons.configuration.Configuration, boolean)}.
     * @throws InterruptedException 
     */
    @Test
    public void testUpdateProperties() throws InterruptedException {
        AbstractConfiguration.setDefaultListDelimiter(',');
        AbstractConfiguration config  = new ConcurrentCompositeConfiguration();
        config.addConfigurationListener(new ExpandedConfigurationListenerAdapter(new MyListener()));
        MyListener.resetCount();
        config.setProperty("test", "host,host1,host2");
        config.setProperty("test12", "host12");
        Map<String,Object> added = Maps.newHashMap();
        added.put("test.host","test,test1");
        Map<String,Object> changed = Maps.newHashMap();
        changed.put("test","host,host1");
        changed.put("test.host","");
        dynamicPropertyUpdater.updateProperties(WatchedUpdateResult.createIncremental(added, changed, null), config, false);
        assertEquals("",config.getProperty("test.host"));
        assertEquals(2,((CopyOnWriteArrayList)(config.getProperty("test"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test"))).contains("host"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test"))).contains("host1"));
        assertEquals(5, MyListener.count);
    }

  
    @Test
    public void testAddorChangeProperty(){
        AbstractConfiguration.setDefaultListDelimiter(',');
        AbstractConfiguration config  = new ConcurrentCompositeConfiguration();
        config.addConfigurationListener(new ExpandedConfigurationListenerAdapter(new MyListener()));
        MyListener.resetCount();
        config.setProperty("test.host", "test,test1,test2");
        assertEquals(1, MyListener.count);
        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1,test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));
        assertEquals(1, MyListener.count);
        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1,test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));
        assertEquals(1, MyListener.count);
        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1", config);
        assertEquals(2,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertEquals(2, MyListener.count);
        
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1,test12", config);
        assertEquals(2,((CopyOnWriteArrayList)(config.getProperty("test.host1"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host1"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host1"))).contains("test12"));
        assertEquals(3, MyListener.count);
        
        config.setProperty("test.host1", "test1.test12");
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1.test12", config);
        assertEquals("test1.test12",config.getProperty("test.host1"));
        assertEquals(4, MyListener.count);
    }
    
    
    @Test
    public void testAddorUpdatePropertyWithColonDelimiter(){
        AbstractConfiguration.setDefaultListDelimiter(':');
        AbstractConfiguration config  = new ConcurrentCompositeConfiguration();
        config.addConfigurationListener(new ExpandedConfigurationListenerAdapter(new MyListener()));
        MyListener.resetCount();
        config.setProperty("test.host", "test:test1:test2");
        assertEquals(1, MyListener.count);
        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test:test1:test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));
        assertEquals(1, MyListener.count); // the config is not set again. when the value is still not changed.
       config.setProperty("test.host1", "test1:test12");
        // changing the new object value , the config.setProperty should be called again.
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1.test12", config);
        assertEquals("test1.test12",config.getProperty("test.host1"));
       assertEquals(3, MyListener.count);
    }

    static class MyListener extends AbstractDynamicPropertyListener {
        static int count = 0;
    
        @Override
        public void handlePropertyEvent(String arg0, Object arg1, EventType arg2) {
            incrementCount();
        }   
        
        private void incrementCount() {
            count++;
        }
    
        protected static void resetCount() {
            count = 0;
        }
    }
}
