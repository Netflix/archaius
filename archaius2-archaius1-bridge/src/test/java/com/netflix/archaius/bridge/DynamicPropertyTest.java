package com.netflix.archaius.bridge;

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
}
