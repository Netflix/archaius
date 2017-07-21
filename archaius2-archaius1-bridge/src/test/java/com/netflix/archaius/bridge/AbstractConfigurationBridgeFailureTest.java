package com.netflix.archaius.bridge;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.config.ConfigurationManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AbstractConfigurationBridgeFailureTest {
    public static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            install(new StaticArchaiusBridgeModule());
            install(new ArchaiusModule());
        }
    }
    
    public static class BadModule extends AbstractModule {
        public static String value = ConfigurationManager.getConfigInstance().getString("foo", "default");
        
        @Override
        protected void configure() {
        }
    }
    
    @Before
    public void before() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        StaticAbstractConfiguration.reset();
        ConfigBasedDeploymentContext.reset();
    }
    
    @Test
    public void testStaticInModule() {
        try {
            Guice.createInjector(
                new TestModule(),
                new BadModule());
            Assert.fail();
        } catch (Exception e) { 
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            
            Assert.assertTrue(stack.contains("com.netflix.archaius.bridge.AbstractConfigurationBridgeFailureTest$BadModule"));
            Assert.assertTrue(stack.contains("**** Remove static reference"));
        }
    }
}
