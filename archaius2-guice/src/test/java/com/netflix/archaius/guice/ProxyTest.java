package com.netflix.archaius.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModuleTest.MyCascadingStrategy;
import com.netflix.archaius.visitor.PrintStreamVisitor;

import org.junit.Assert;
import org.junit.Test;

import javax.inject.Singleton;

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
    public void testConfigWithNoPrefix() throws ConfigException {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                @Override
                protected void configureArchaius() {
                    this.bindApplicationConfigurationOverride().toInstance(MapConfig.builder()
                            .put("integer", 1)
                            .put("string", "bar")
                            .put("subConfig.integer", 2)
                            .build());
                }
                
                @Provides
                @Singleton
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class);
                }
            }
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
    public void testConfigWithProvidedPrefix() throws ConfigException {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                @Override
                protected void configureArchaius() {
                    this.bindApplicationConfigurationOverride().toInstance(MapConfig.builder()
                    .put("prefix.integer", 1)
                    .put("prefix.string", "bar")
                    .build());
                }
                
                @Provides
                @Singleton
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class, "prefix");
                }
            });
        
        MyConfig config = injector.getInstance(MyConfig.class);
        Assert.assertEquals("bar", config.getString());
        Assert.assertEquals(1, config.getInteger());
    }
    
    @Configuration(prefix="prefix-${env}", allowFields=true)
    @ConfigurationSource(value={"moduleTest"}, cascading=MyCascadingStrategy.class)
    public static interface ModuleTestConfig {
        public Boolean isLoaded();
        public String getProp1();
    }
    
    @Test
    public void confirmConfigurationSourceWorksWithProxy() {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                @Provides
                @Singleton
                public ModuleTestConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(ModuleTestConfig.class, "moduleTest");
                }
            });
            
        ModuleTestConfig config = injector.getInstance(ModuleTestConfig.class);
        Assert.assertTrue(config.isLoaded());
        Assert.assertEquals("fromFile", config.getProp1());
        
        injector.getInstance(Config.class).accept(new PrintStreamVisitor());
    }
}
