package com.netflix.archaius.bridge;

import com.netflix.archaius.api.Config;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class DynamicPropertyTest {
    @Test
    public void test() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        Property<String> prop2 = injector.getInstance(PropertyFactory.class).getProperty("foo").asString("default");
        
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty("foo", "default");
        Assert.assertEquals("default", prop.get());
        Assert.assertEquals("default", prop2.get());
        
        SettableConfig config = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        config.setProperty("foo", "newvalue");
        
        Assert.assertEquals("newvalue", prop.get());
        Assert.assertEquals("newvalue", prop2.get());
        
    }
    
    @Test
    public void testNonStringDynamicProperty() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        ConfigurationManager.getConfigInstance().setProperty("foo", 123);
        
        Property<Integer> prop2 = injector.getInstance(PropertyFactory.class).getProperty("foo").asInteger(1);
        
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty("foo", 2);
        
        Assert.assertEquals(123, prop.get());
        Assert.assertEquals(123, (int)prop2.get());
        
    }
    
    @Test
    public void testInterpolation() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        ConfigurationManager.getConfigInstance().setProperty("foo", "${bar}");
        ConfigurationManager.getConfigInstance().setProperty("bar", "value");
        
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty("foo", "default");
        
        Assert.assertEquals("value", prop.get());
        
    }

    @Test
    public void testPropertyDeletion() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());
        AbstractConfiguration config1 = ConfigurationManager.getConfigInstance();
        Config config2 = injector.getInstance(Config.class);
        config1.setProperty("libA.loaded", "true");
        Assert.assertTrue(config1.getBoolean("libA.loaded",  false));
        Assert.assertTrue(config2.getBoolean("libA.loaded",  false));

        config1.clearProperty("libA.loaded");
        Assert.assertFalse(config1.getBoolean("libA.loaded",  false));
        Assert.assertFalse(config2.getBoolean("libA.loaded",  false));

    }
}
