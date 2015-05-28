package com.netflix.archaius.bridge;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.inject.RuntimeLayer;
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
}
