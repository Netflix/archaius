package com.netflix.config;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class MissingConfigExceptionTest {

    @BeforeClass
    public static void init() {
        System.setProperty(DynamicPropertyFactory.THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION, "true");
    }
    
    @Test
    public void testThrowMissingConfigurationSourceException() {
        try {
            DynamicPropertyFactory.getInstance();
            fail("MissingConfigurationSourceException expected");
        } catch (MissingConfigurationSourceException e) {
            assertNotNull(e);
        }
        DynamicPropertyFactory.setThrowMissingConfigurationSourceException(false);
        try {
            DynamicPropertyFactory.getInstance();            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
    }
}
