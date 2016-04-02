package com.netflix.archaius.bridge;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.config.ConfigurationManager;

public class Arcahsiu2BackportModuleTest {
    
    public static interface MyConfiguration {
        String getString();
        int getInteger();
        int getStringInteger();
        int getInterpolatedInteger();
    }
    
    public static class MyConfigurationModule extends AbstractModule {
        @Override
        protected void configure() {
        }
        
        @Provides
        @Singleton
        MyConfiguration getConfiguration(ConfigProxyFactory factory) {
            return factory.newProxy(MyConfiguration.class);
        }
    }
    
    @Test
    public void stringPropertyBinding() {
        ConfigurationManager.getConfigInstance().setProperty("string", "value");
        
        Injector injector = Guice.createInjector(new Archaius2BackportModule(), new MyConfigurationModule());
        
        MyConfiguration config = injector.getInstance(MyConfiguration.class);
        
        Assert.assertEquals("value", config.getString());
    }
    
    @Test
    public void nonStringPropertyBinding() {
        ConfigurationManager.getConfigInstance().setProperty("integer", 123);
        ConfigurationManager.getConfigInstance().setProperty("stringInteger", "124");
        
        Injector injector = Guice.createInjector(new Archaius2BackportModule(), new MyConfigurationModule());
        
        MyConfiguration config = injector.getInstance(MyConfiguration.class);
        
        Assert.assertEquals(123, config.getInteger());
        Assert.assertEquals(124, config.getStringInteger());
    }
    
    @Test
    public void interpolatedValueBinding() {
        ConfigurationManager.getConfigInstance().setProperty("integer", 123);
        ConfigurationManager.getConfigInstance().setProperty("interpolatedInteger", "${integer}");
        
        Injector injector = Guice.createInjector(new Archaius2BackportModule(), new MyConfigurationModule());
        
        MyConfiguration config = injector.getInstance(MyConfiguration.class);
        
        Assert.assertEquals(123, config.getInterpolatedInteger());
    }
    
    @Test
    public void proxiedInterfaceUpdates() {
        ConfigurationManager.getConfigInstance().setProperty("string", "value");
        
        Injector injector = Guice.createInjector(new Archaius2BackportModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                }
                
                @Provides
                @Singleton
                MyConfiguration getConfiguration(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfiguration.class);
                }
        });
        
        MyConfiguration config = injector.getInstance(MyConfiguration.class);
        
        Assert.assertEquals("value", config.getString());
        
        ConfigurationManager.getConfigInstance().setProperty("string", "new_value");
        Assert.assertEquals("new_value", config.getString());
        
    }
}
