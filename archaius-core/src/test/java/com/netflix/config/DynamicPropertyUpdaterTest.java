package com.netflix.config;

import static org.junit.Assert.*;

import com.google.common.collect.Maps;
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
        
        Configuration config  = new ConcurrentCompositeConfiguration();
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
        
       
    }

  
    @Test
    public void testAddorChangeProperty(){
        AbstractConfiguration.setDefaultListDelimiter(',');
        
        Configuration config  = new ConcurrentCompositeConfiguration();
        config.setProperty("test.host", "test,test1,test2");

        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1,test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));
        
        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1,test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));

        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test,test1", config);
        assertEquals(2,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
       
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1,test12", config);
        assertEquals(2,((CopyOnWriteArrayList)(config.getProperty("test.host1"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host1"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host1"))).contains("test12"));
        
        config.setProperty("test.host1", "test1.test12");
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1.test12", config);
        assertEquals("test1.test12",config.getProperty("test.host1"));
        
    }
    
    
    @Test
    public void testAddorUpdatePropertyWithColonDelimiter(){
        AbstractConfiguration.setDefaultListDelimiter(':');
        Configuration config  = new ConcurrentCompositeConfiguration();
        config.setProperty("test.host", "test:test1:test2");

        dynamicPropertyUpdater.addOrChangeProperty("test.host", "test:test1:test2", config);
        assertEquals(3,((CopyOnWriteArrayList)(config.getProperty("test.host"))).size());
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test1"));
        assertTrue(((CopyOnWriteArrayList)(config.getProperty("test.host"))).contains("test2"));
        
        config.setProperty("test.host1", "test1:test12");
        // changing the new object value
        dynamicPropertyUpdater.addOrChangeProperty("test.host1", "test1.test12", config);
        assertEquals("test1.test12",config.getProperty("test.host1"));
        
    }

}
