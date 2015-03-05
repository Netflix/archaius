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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.netflix.config.sources.URLConfigurationSource;
import com.netflix.config.validation.ValidationException;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DynamicFileConfigurationTest {

    boolean propertyChanged = false;
    
    static DynamicPropertyFactory propertyFactory = null;
    
    private DynamicLongProperty longProp = null;
    
    private static DynamicProperty dynProp = null;
    
    static File configFile = null;
    static File createConfigFile(String prefix) throws Exception {
        configFile = File.createTempFile(prefix, ".properties");        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
        writer.write("dprops1=123456789");
        writer.newLine();
        writer.write("dprops2=79.98");
        writer.newLine();
        writer.write("abc=-2"); // this property should fail validation but should not affect update of other properties
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
                    writer.write("abc=-8"); // this property should fail validation but should not affect update of other properties
                    writer.newLine();
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

	static class Listener implements ConfigurationListener {

		volatile ConfigurationEvent lastEventBeforeUpdate;
		
		volatile ConfigurationEvent lastEventAfterUpdate;
		
		AtomicInteger counter = new AtomicInteger();

		@Override
		public void configurationChanged(ConfigurationEvent event) {
			System.out.println("Event received: " + event.getType() + "," + event.getPropertyName() + "," + event.isBeforeUpdate() + "," + event.getPropertyValue());
			counter.incrementAndGet();
			if (event.isBeforeUpdate()) {
				lastEventBeforeUpdate = event;
			} else {
				lastEventAfterUpdate = event;
			}
		}
		
		public void clear() {
			lastEventBeforeUpdate = null;
			lastEventAfterUpdate = null;
		}
		
		public ConfigurationEvent getLastEvent(boolean beforeUpdate) {
			if (beforeUpdate) {
				return lastEventBeforeUpdate;
			} else {
				return lastEventAfterUpdate;
			}
		}
		
		public int getCount() {
			return counter.get();
		}
	}

	static Listener listener = new Listener();
	
    @BeforeClass
    public static void init() throws Exception {
        String path = createConfigFile("configFile").toURI().toURL().toString();
        System.setProperty(URLConfigurationSource.CONFIG_URL, path);
        System.setProperty(FixedDelayPollingScheduler.INITIAL_DELAY_PROPERTY, "0");
        System.setProperty(FixedDelayPollingScheduler.DELAY_PROPERTY, "100");
        dynProp = DynamicProperty.getInstance("dprops1");
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
        longProp = propertyFactory.getLongProperty("dprops1", Long.MAX_VALUE, new Runnable() {
            public void run() {
                propertyChanged = true;
            }
        });

        DynamicIntProperty validatedProp = new DynamicIntProperty("abc", 0) {
            @Override
            public void validate(String newValue) {
                if (Integer.parseInt(newValue) < 0) {
                    throw new ValidationException("Cannot be negative");
                }
            }
        };
        assertEquals(0, validatedProp.get());
        assertFalse(propertyChanged);
        DynamicDoubleProperty doubleProp = propertyFactory.getDoubleProperty("dprops2", 0.0d);
        assertEquals(123456789, longProp.get());
        assertEquals(123456789, dynProp.getInteger().intValue());
        assertEquals(79.98, doubleProp.get(), 0.00001d);
        assertEquals(Double.valueOf(79.98), doubleProp.getValue());
        assertEquals(Long.valueOf(123456789L), longProp.getValue());
        modifyConfigFile();
        ConfigurationManager.getConfigInstance().addConfigurationListener(listener);
        Thread.sleep(1000);
        assertEquals(Long.MIN_VALUE, longProp.get());
        assertEquals(0, validatedProp.get());
        assertTrue(propertyChanged);
        assertEquals(Double.MAX_VALUE, doubleProp.get(), 0.01d);
        assertFalse(ConfigurationManager.isConfigurationInstalled());
        Thread.sleep(3000);
        // Only 4 events expected, two each for dprops1 and dprops2
        assertEquals(4, listener.getCount());
    }    
    
    @Test
    public void testSwitchingToConfiguration() throws Exception {
        longProp = propertyFactory.getLongProperty("dprops1", Long.MAX_VALUE, new Runnable() {
            public void run() {
                propertyChanged = true;
            }
        });
        AbstractConfiguration newConfig = new ConcurrentMapConfiguration();
        DynamicStringProperty prop = propertyFactory.getStringProperty("abc", "default");
        newConfig.setProperty("abc", "nondefault");
        newConfig.setProperty("dprops1", "0");
        DynamicPropertyFactory.initWithConfigurationSource(newConfig);
        Thread.sleep(2000);
        assertEquals("nondefault", prop.get());
        assertEquals(0, longProp.get());
        assertTrue(newConfig == ConfigurationManager.getConfigInstance());
        assertTrue(ConfigurationManager.isConfigurationInstalled());
    }
}
