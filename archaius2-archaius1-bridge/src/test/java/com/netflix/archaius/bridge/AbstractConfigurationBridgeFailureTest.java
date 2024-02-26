package com.netflix.archaius.bridge;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.config.ConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    
    @BeforeEach
    public void before() throws SecurityException {
        StaticAbstractConfiguration.reset();
        StaticDeploymentContext.reset();
    }
    
    @Test
    public void testStaticInModule() {
        Exception exception = assertThrows(Exception.class, () ->
                Guice.createInjector(
                        new TestModule(),
                        new BadModule()));
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
            
        assertTrue(stack.contains("com.netflix.archaius.bridge.AbstractConfigurationBridgeFailureTest$BadModule"));
        assertTrue(stack.contains("**** Remove static reference"));
    }
}
