package com.netflix.config;

import com.google.common.collect.ImmutableMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;

public class DynamicPropertyUsedSettingRegistryTest {

    private static File configFile;
    private static DynamicConfiguration config;

    @BeforeClass
    public static void init() throws Exception {
        configFile = DynamicPropertyUtils.createConfigFile(ImmutableMap.<String, Object>of(
                "prop1", "test1",
                "prop2", "test2",
                "prop3", "test3",
                "prop4", "test4"
        ));
        config = new DynamicURLConfiguration(100, 500, false, configFile.toURI().toURL().toString());
        System.out.println("Initializing with sources: " + config.getSource());
        DynamicPropertyFactory.initWithConfigurationSource(config);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (!configFile.delete()) {
            System.err.println("Unable to delete file " + configFile.getPath());
        }
    }

    @Test
    public void onlyRequestedSettingsShouldBeHeldByUsedSettingsRegistry() throws Exception {
        DynamicStringProperty prop1 = new DynamicStringProperty("prop1", null);
        DynamicStringProperty prop2 = new DynamicStringProperty("prop2", null);

        assertEquals("test1", prop1.get());
        assertEquals("test2", prop2.get());

        assertEquals(of(
                "prop1", "test1",
                "prop2", "test2"),
                UsedSettingsRegistry.instance().getSettings());


    }
}
