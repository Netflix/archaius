package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Test;

public class PropertyListenerTest {
    
    @Test
    public void testAddPropertyListener() {
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        config.addConfigurationListener(new ExpandedConfigurationListenerAdapter(new MyListener()));
        // config.addConfigurationListener(new MyListener());
        config.setProperty("xyz", "abcc");
        assertEquals(1, MyListener.count);
    }

}

class MyListener extends AbstractDynamicPropertyListener {
    static int count = 0;

    @Override
    public void handlePropertyEvent(String arg0, Object arg1, EventType arg2) {
        incrementCount();
    }   
    
    private void incrementCount() {
        count++;
    }
}
