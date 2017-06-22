package com.netflix.archaius.bridge;

import com.netflix.archaius.api.Config;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.visitor.PrintStreamVisitor;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class DynamicPropertyTest {
    @Rule
    public TestName testName = new TestName();
    
    @Test
    public void settingOnArchaius2UpdateArchaius1() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        Property<String> a2prop = injector.getInstance(PropertyFactory.class).getProperty(testName.getMethodName()).asString("default");
        DynamicStringProperty a1prop = DynamicPropertyFactory.getInstance().getStringProperty(testName.getMethodName(), "default");
        
        Assert.assertEquals("default", a1prop.get());
        Assert.assertEquals("default", a2prop.get());
        
        SettableConfig config = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        config.setProperty(testName.getMethodName(), "newvalue");
        
        Assert.assertEquals("newvalue", a2prop.get());
        Assert.assertEquals("newvalue", a1prop.get());
    }
    
    @Test
    public void testNonStringDynamicProperty() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        Config config = injector.getInstance(Config.class);
        config.accept(new PrintStreamVisitor());
        ConfigurationManager.getConfigInstance().setProperty("foo", 123);
        
        Property<Integer> prop2 = injector.getInstance(PropertyFactory.class).getProperty("foo").asInteger(1);
        
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty("foo", 2);
        
        Assert.assertEquals(123, prop.get());
        Assert.assertEquals(123, (int)prop2.get());
        
    }
    
    @Test
    public void testInterpolation() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());

        Config config = injector.getInstance(Config.class);
        config.forEachProperty((k, v) -> System.out.println(k + " = " + v));
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
