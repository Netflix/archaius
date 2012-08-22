package com.netflix.config;

import static org.junit.Assert.*;

import org.junit.Test;

import com.netflix.config.DynamicURLConfiguration;
import com.netflix.config.sources.URLConfigurationSource;

public class DynamicURLConfigurationTest {

    @Test
    public void testNoURLsAvailable() {
        try {
            DynamicURLConfiguration config = new DynamicURLConfiguration();
            assertFalse(config.getKeys().hasNext());
        } catch (Throwable e) {
            fail("Unexpected exception");
        }
        
    }
}
