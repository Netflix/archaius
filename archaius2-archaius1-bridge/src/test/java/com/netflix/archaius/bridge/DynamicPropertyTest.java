package com.netflix.archaius.bridge;

import org.apache.commons.configuration.AbstractConfiguration;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicPropertyTest {
    
    private Injector injector;
    
    @BeforeEach
    public void before() {
        injector = Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());
    }
    
    @AfterEach
    public void after() {
        StaticArchaiusBridgeModule.resetStaticBridges();
    }
    
    @Test
    public void settingOnArchaius2UpdateArchaius1(TestInfo testInfo) {
        String methodName = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        Property<String> a2prop = injector.getInstance(PropertyFactory.class).getProperty(methodName).asString("default");
        DynamicStringProperty a1prop = DynamicPropertyFactory.getInstance().getStringProperty(methodName, "default");
        
        assertEquals("default", a1prop.get());
        assertEquals("default", a2prop.get());
        
        SettableConfig config = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        config.setProperty(methodName, "newvalue");

        assertEquals("newvalue", a2prop.get());
        assertEquals("newvalue", a1prop.get());
    }
    
    @Test
    public void testNonStringDynamicProperty() {
        Config config = injector.getInstance(Config.class);
        config.accept(new PrintStreamVisitor());
        ConfigurationManager.getConfigInstance().setProperty("foo", 123);
        
        Property<Integer> prop2 = injector.getInstance(PropertyFactory.class).getProperty("foo").asInteger(1);
        
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty("foo", 2);
        
        assertEquals(123, (int)prop2.get());
        assertEquals(123, prop.get());
    }
    
    @Test
    public void testInterpolation() {
        Config config = injector.getInstance(Config.class);
        config.forEachProperty((k, v) -> System.out.println(k + " = " + v));
        ConfigurationManager.getConfigInstance().setProperty("foo", "${bar}");
        ConfigurationManager.getConfigInstance().setProperty("bar", "value");
        
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty("foo", "default");
        
        assertEquals("value", prop.get());
        
    }

    @Test
    public void testPropertyDeletion() {
        AbstractConfiguration config1 = ConfigurationManager.getConfigInstance();
        Config config2 = injector.getInstance(Config.class);
        config1.setProperty("libA.loaded", "true");
        assertTrue(config1.getBoolean("libA.loaded",  false));
        assertTrue(config2.getBoolean("libA.loaded",  false));

        config1.clearProperty("libA.loaded");
        assertFalse(config1.getBoolean("libA.loaded",  false));
        assertFalse(config2.getBoolean("libA.loaded",  false));

    }
}
