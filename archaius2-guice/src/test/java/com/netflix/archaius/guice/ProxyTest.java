package com.netflix.archaius.guice;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.annotations.DefaultValue;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.inject.RuntimeLayer;

public class ProxyTest {
    public static interface MyConfig {
        @DefaultValue("0")
        int getInteger();
        
        String getString();
        
        MySubConfig getSubConfig();
    }
    
    public static interface MySubConfig {
        @DefaultValue("0")
        int getInteger();
    }
    
    @Configuration(prefix="foo")
    public static interface MyConfigWithPrefix {
        @DefaultValue("0")
        int getInteger();
        
        String getString();
    }
    
    @Test
    public void testConfigWithNoPrefix() {
        Injector injector = Guice.createInjector(Modules
            .override(new ArchaiusModule())
            .with(new AbstractModule() {
                @Override
                protected void configure() {
                    ConfigSeeders.bind(binder(), 
                            MapConfig.<String>builder()

                                    .put("integer", "1")
                                .put("string", "bar")
                                .put("subConfig.integer", "2")
                                .build(), 
                            RuntimeLayer.class);
                }
                
                @Provides
                @Singleton
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class);
                }
            })
        );
        
        SettableConfig cfg = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        MyConfig config = injector.getInstance(MyConfig.class);
        Assert.assertEquals("bar", config.getString());
        Assert.assertEquals(1, config.getInteger());
        Assert.assertEquals(2, config.getSubConfig().getInteger());
        
        cfg.setProperty("subConfig.integer", 3);
        
        Assert.assertEquals(3, config.getSubConfig().getInteger());
    }
    
    @Test
    public void testConfigWithProvidedPrefix() {
        Injector injector = Guice.createInjector(Modules
            .override(new ArchaiusModule())
            .with(new AbstractModule() {
                @Override
                protected void configure() {
                    ConfigSeeders.bind(binder(), 
                            MapConfig.<String>builder()
                                .put("prefix.integer", "1")
                                .put("prefix.string", "bar")
                                .build(), 
                            RuntimeLayer.class);
                }
                
                @Provides
                @Singleton
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class, "prefix");
                }
            })
            );
        
        MyConfig config = injector.getInstance(MyConfig.class);
        Assert.assertEquals("bar", config.getString());
        Assert.assertEquals(1, config.getInteger());
        
    }
}
