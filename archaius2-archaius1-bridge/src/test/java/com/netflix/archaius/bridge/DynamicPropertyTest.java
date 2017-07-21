package com.netflix.archaius.bridge;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class DynamicPropertyTest {
    static Injector injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());
    
    @Rule
    public TestName name = new TestName();
    
    @Test
    public void testBridge() {
        String propName = name.getMethodName() + "foo";
        
        Property<String> prop2 = injector.getInstance(PropertyFactory.class)
                .getProperty(propName)
                .asString("default");
        
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty(propName, "default");
        Assert.assertEquals("default", prop.get());
        Assert.assertEquals("default", prop2.get());
        
        SettableConfig config = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        config.setProperty(propName, "newvalue");
        
        Assert.assertEquals("newvalue", prop.get());
        Assert.assertEquals("newvalue", prop2.get());
    }
    
    @Test
    public void testNonStringDynamicProperty() {
        String propName = name.getMethodName() + "foo";

        ConfigurationManager.getConfigInstance().setProperty(propName, 123);
        
        Property<Integer> prop2 = injector.getInstance(PropertyFactory.class).getProperty(propName).asInteger(1);
        
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty(propName, 2);
        
        Assert.assertEquals(123, prop.get());
        Assert.assertEquals(123, (int)prop2.get());
        
    }
    
    @Test
    public void testInterpolation() {
        String propName = name.getMethodName() + "foo";
        
        ConfigurationManager.getConfigInstance().setProperty(propName, "${bar}");
        ConfigurationManager.getConfigInstance().setProperty("bar", "value");
        
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty(propName, "default");
        
        Assert.assertEquals("value", prop.get());
        
    }
}
