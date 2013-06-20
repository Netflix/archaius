/**
 * Copyright 2013 Netflix, Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DynamicPropertyTest {

    static File configFile;    
    private static final String PROP_NAME = "biz.mindyourown.notMine";
    private static final String PROP_NAME2 = "biz.mindyourown.myProperty";
    private static DynamicConfiguration config;
    boolean meGotCalled = false;
    
    static void createConfigFile() throws Exception {
        configFile = File.createTempFile("config", "properties");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
        writer.write("props1=xyz");
        writer.newLine();
        writer.write("props2=abc");
        writer.newLine();
        writer.close();
    }
    
    static void modifyConfigFile() throws Exception {
        new Thread() {
            public void run() {
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
                    writer.write("props2=456");
                    writer.newLine();
                    writer.write("props3=123");
                    writer.close();      
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                }
            }
        }.start();
    }
    
    @BeforeClass
    public static void init() throws Exception {
        createConfigFile();
        config = new DynamicURLConfiguration(100, 500, false, configFile.toURI().toURL().toString());
        System.out.println("Initializing with sources: " + config.getSource());
        DynamicPropertyFactory.initWithConfigurationSource(config);
        // Create new DynamicFileConfiguration
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        if (!configFile.delete()) {
            System.err.println("Unable to delete file " + configFile.getPath());
        }
    }
    
    @Test
    public void testAsFileBased() throws Exception {
        
        // TODO: create a static DynamicProperties class
        DynamicStringProperty prop = new DynamicStringProperty("props1", null);
        DynamicStringProperty prop2 = new DynamicStringProperty("props2", null);
        DynamicIntProperty prop3 = new DynamicIntProperty("props3", 0);
        Thread.sleep(1000);
        assertEquals("xyz", prop.get());    
        assertEquals("abc", prop2.get());  
        assertEquals(0, prop3.get());
        modifyConfigFile();
        // waiting for reload
        Thread.sleep(2000);
        assertNull(prop.get());
        assertEquals("456", prop2.get());
        assertEquals(123, prop3.get());
        config.stopLoading();
        Thread.sleep(2000);
        config.setProperty("props2", "000");
        assertEquals("000", prop2.get());
    }
        
    @Test
    public void testDynamicProperty() {
        config.stopLoading();
        DynamicProperty fastProp = DynamicProperty.getInstance(PROP_NAME);
        assertEquals("FastProperty does not have correct name",
                PROP_NAME, fastProp.getName());
        assertSame("DynamicProperty.getInstance did not find the object",
                fastProp, DynamicProperty.getInstance(PROP_NAME));
        //
        String hello = "Hello";
        assertNull("Unset DynamicProperty is not null",
                fastProp.getString());
        assertEquals("Unset DynamicProperty does not default correctly",
                     hello, fastProp.getString(hello));
        config.setProperty(PROP_NAME, hello);
        assertEquals("Set DynamicProperty does not have correct value",
                     hello, fastProp.getString());
        assertEquals("Set DynamicProperty uses supplied default",
                     hello, fastProp.getString("not " + hello));
        assertEquals("Non-integer DynamicProperty doesn't default on integer fetch",
                     123, fastProp.getInteger(Integer.valueOf(123)).intValue());
        assertEquals("Non-float DynamicProperty doesn't default on float fetch",
                     2.71838f, fastProp.getFloat(Float.valueOf(2.71838f)).floatValue(), 0.001f);
        try {
            fastProp.getFloat();
            fail("Parse should have failed:  " + fastProp);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        //
        String pi = "3.14159";
        String ee = "2.71838";
        config.setProperty(PROP_NAME, pi);
        assertEquals("Set DynamicProperty does not have correct value",
                     pi, fastProp.getString());
        assertEquals("DynamicProperty did not property parse float string",
                     3.14159f, fastProp.getFloat(Float.valueOf(0.0f)).floatValue(), 0.001f);
        config.setProperty(PROP_NAME, ee);
        assertEquals("Set DynamicProperty does not have correct value",
                     ee, fastProp.getString());
        assertEquals("DynamicProperty did not property parse float string",
                     2.71838f, fastProp.getFloat(Float.valueOf(0.0f)).floatValue(), 0.001f);
        try {
            fastProp.getInteger();
            fail("Integer fetch of non-integer DynamicProperty should have failed:  " + fastProp);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        assertEquals("Integer fetch of non-integer DynamicProperty did not use default value",
                    -123, fastProp.getInteger(Integer.valueOf(-123)).intValue());
        //
        String devil = "666";
        config.setProperty(PROP_NAME, devil);
        assertEquals("Changing DynamicProperty does not result in correct value",
                     devil, fastProp.getString());
        assertEquals("Integer fetch of changed DynamicProperty did not return correct value",
                     666, fastProp.getInteger().intValue());
        //
        String self = "com.netflix.config.DynamicProperty";
        assertEquals("Fetch of named class from integer valued DynamicProperty did not use default",
                     DynamicPropertyTest.class, fastProp.getNamedClass(DynamicPropertyTest.class));
        config.setProperty(PROP_NAME, self);
        assertEquals("Fetch of named class from DynamicProperty did not find the class",
                     DynamicProperty.class, fastProp.getNamedClass());
        // Check that clearing a property clears all caches
        config.clearProperty(PROP_NAME);
        assertNull("Fetch of cleard property did not return null",
                   fastProp.getString());
        assertEquals("Fetch of cleard property did not use default value",
                     devil, fastProp.getString(devil));
        assertNull("Fetch of cleard property did not return null",
                   fastProp.getInteger());
        assertEquals("Fetch of cleard property did not use default value",
                    -123, fastProp.getInteger(Integer.valueOf(-123)).intValue());
        assertNull("Fetch of cleard property did not return null",
                   fastProp.getFloat());
        assertEquals("Fetch of cleard property did not use default value",
                     2.71838f, fastProp.getFloat(Float.valueOf(2.71838f)).floatValue(), 0.001f);
        assertNull("Fetch of cleard property did not return null",
                   fastProp.getNamedClass());
        assertEquals("Fetch of cleard property did not use default value",
                     DynamicProperty.class, fastProp.getNamedClass(DynamicProperty.class));
        //
        String yes = "yes";
        String maybe = "maybe";
        String no = "Off";
        config.setProperty(PROP_NAME, yes);
        assertTrue("boolean property set to 'yes' is not true",
                fastProp.getBoolean().booleanValue());
        config.setProperty(PROP_NAME, no);
        assertTrue("boolean property set to 'no' is not false",
                   !fastProp.getBoolean().booleanValue());
        config.setProperty(PROP_NAME, maybe);
        try {
            fastProp.getBoolean();
            fail("Parse should have failed: " + fastProp);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        assertTrue(fastProp.getBoolean(Boolean.TRUE).booleanValue());
        assertTrue(!fastProp.getBoolean(Boolean.FALSE).booleanValue());
    }

    @Test
    public void testPerformance() {
        config.stopLoading();
        DynamicProperty fastProp = DynamicProperty.getInstance(PROP_NAME2);
        String goodbye = "Goodbye";
        int loopCount = 1000000;
        config.setProperty(PROP_NAME2, goodbye);
        long cnt = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            cnt += fastProp.getString().length();
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Fetched dynamic property " + loopCount + " times in " + elapsed + " milliseconds");
        // Now for the "normal" time
        cnt = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            cnt += config.getString(PROP_NAME2).length();
        }
        elapsed = System.currentTimeMillis() - start;
        System.out.println("Fetched Configuration value " + loopCount + " times in " + elapsed + " milliseconds");
        // Now for the "system property" time
        cnt = 0;
        System.setProperty(PROP_NAME2, goodbye);
        start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            cnt += System.getProperty(PROP_NAME2).length();
        }
        elapsed = System.currentTimeMillis() - start;
        System.out.println("Fetched system property value " + loopCount + " times in " + elapsed + " milliseconds");
    }


    @Test
    public void testDynamicPropertyListenerPropertyChangeCallback(){
        config.stopLoading();
        DynamicStringProperty listOfCountersToExportProperty =
            new DynamicStringProperty("com.netflix.eds.utils.EdsCounter.listOfCountersToExport", "") {
                @Override
                protected void propertyChanged() {
                    meGotCalled = true;
                }
            };
        config.setProperty("com.netflix.eds.utils.EdsCounter.listOfCountersToExport", "valuechanged");
        assertTrue("propertyChanged did not get called", meGotCalled);
        assertEquals("valuechanged", listOfCountersToExportProperty.get());
    }

    @Test
    public void testFastProperyTimestamp() throws Exception {
        config.stopLoading();
        DynamicStringProperty prop = new DynamicStringProperty("com.netflix.testing.timestamp", "hello");
        long initialTime = prop.getChangedTimestamp();
        Thread.sleep(10);
        assertEquals(prop.getChangedTimestamp(), initialTime);
        config.setProperty(prop.getName(), "goodbye");
        assertTrue((prop.getChangedTimestamp() - initialTime) > 8);
    }

    @Test
    public void testDynamicProperySetAdnGets() throws Exception {
        config.stopLoading();
        DynamicBooleanProperty prop = new DynamicBooleanProperty(
                "com.netflix.testing.mybool", false);
        assertFalse(prop.get());
        assertTrue(prop.prop.getCallbacks().isEmpty());
        for (int i = 0; i < 10; i++) {
           
            config.setProperty(
                    "com.netflix.testing.mybool", "true");
            assertTrue(prop.get());
            assertTrue(config.getString("com.netflix.testing.mybool").equals("true"));
            config.setProperty(
                    "com.netflix.testing.mybool", "false");            
            assertFalse(prop.get());
            assertTrue(config.getString("com.netflix.testing.mybool").equals("false"));
        }
        for(int i = 0; i < 100; i++) {
            config.setProperty(
                    "com.netflix.testing.mybool", "true");
            assertTrue(prop.get());
            assertTrue(config.getString("com.netflix.testing.mybool").equals("true"));
            config.clearProperty(
                    "com.netflix.testing.mybool");
            assertFalse(prop.get());
            assertTrue(config.getString("com.netflix.testing.mybool") == null);
        }
    }
    
    @Test
    public void testPropertyCreation() {
        config.stopLoading();
        meGotCalled = false;
        final String newValue = "newValue";
        Runnable r = new Runnable() {
            public void run() {
                meGotCalled = true;
            }            
        };
        final DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty("foo.bar", "xyz", r);
        assertEquals("xyz", prop.get());
        config.setProperty("foo.bar", newValue);
        assertTrue(meGotCalled);
        assertEquals(newValue, prop.get());
        assertTrue(prop.prop.getCallbacks().contains(r));
    }


}
