package com.netflix.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DynamicFileConfigurationTest {

    boolean propertyChanged = false;
    
    static DynamicPropertyFactory propertyFactory = null;
    
    private DynamicLongProperty longProp = null;
    
    static File configFile = null;
    static File createConfigFile(String prefix) throws Exception {
        configFile = File.createTempFile(prefix, ".properties");        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
        writer.write("dprops1=123456789");
        writer.newLine();
        writer.write("dprops2=79.98");
        writer.newLine();
        writer.close();
        System.err.println(configFile.getPath() + " created");
        return configFile;
    }
    
    static void modifyConfigFile() {
        new Thread() {
            public void run() {
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
                    writer.write("dprops1=" + String.valueOf(Long.MIN_VALUE)); 
                    writer.newLine();
                    writer.write("dprops2=" + String.valueOf(Double.MAX_VALUE));
                    writer.newLine();
                    writer.close();   
                    System.err.println(configFile.getPath() + " modified");
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                }
            }
        }.start();
    }

    @BeforeClass
    public static void init() throws Exception {
        String path = createConfigFile("configFile").toURI().toURL().toString();
        System.setProperty(URLConfigurationSource.CONFIG_URL, path);
        System.setProperty(FixedDelayPollingScheduler.INITIAL_DELAY_PROPERTY, "0");
        System.setProperty(FixedDelayPollingScheduler.DELAY_PROPERTY, "100");
        propertyFactory = DynamicPropertyFactory.getInstance();
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        if (!configFile.delete()) {
            System.err.println("Unable to delete file " + configFile.getPath());
        }
    }
    
    @Test
    public void testDefaultConfigFile() throws Exception {
        longProp = propertyFactory.createLongProperty("dprops1", Long.MAX_VALUE, new Runnable() {
            public void run() {
                propertyChanged = true;
            }
        });

        assertFalse(propertyChanged);
        DynamicDoubleProperty doubleProp = propertyFactory.createDoubleProperty("dprops2", 0.0d);
        assertEquals(123456789, longProp.get());
        assertEquals(79.98, doubleProp.get(), 0.00001d);
        assertEquals(Double.valueOf(79.98), doubleProp.getValue());
        assertEquals(Long.valueOf(123456789L), longProp.getValue());
        modifyConfigFile();
        Thread.sleep(1000);
        assertEquals(Long.MIN_VALUE, longProp.get());
        assertTrue(propertyChanged);
        assertEquals(Double.MAX_VALUE, doubleProp.get(), 0.01d);
    }    
    
    @Test
    public void testSwitchingToConfiguration() throws Exception {
        longProp = propertyFactory.createLongProperty("dprops1", Long.MAX_VALUE, new Runnable() {
            public void run() {
                propertyChanged = true;
            }
        });
        AbstractConfiguration newConfig = new ConcurrentMapConfiguration();
        DynamicStringProperty prop = propertyFactory.createStringProperty("abc", "default");
        newConfig.setProperty("abc", "nondefault");
        newConfig.setProperty("dprops1", "0");
        DynamicPropertyFactory.initWithConfigurationSource(newConfig);
        Thread.sleep(2000);
        assertEquals("nondefault", prop.get());
        assertEquals(0, longProp.get());
    }
}
