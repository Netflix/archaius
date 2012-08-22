package com.netflix.config;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class DynamicURLConfigurationTestWithFileURL {
    
    @BeforeClass
    public static void init() {
        System.setProperty("archaius.configurationSource.defaultFileName", "sampleapp.properties");
    }
    
    @Test
    public void testFileURL() {
        DynamicURLConfiguration config = new DynamicURLConfiguration();
        assertEquals(5, config.getInt("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
    }
}
