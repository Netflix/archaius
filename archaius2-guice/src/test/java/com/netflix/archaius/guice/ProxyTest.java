package com.netflix.archaius.guice;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.config.MapConfig;

public class ProxyTest {
    public static interface MyConfig {
        @DefaultValue("0")
        int getInteger();
        
        String getString();
        
        MySubConfig getSubConfig();
        
        default String getDefault() {
            return getInteger() + "-" + getString();
        }
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
        Assert.assertEquals("1-bar", config.getDefault());
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
    
    public static interface DefaultMethodWithAnnotation {
        @DefaultValue("fromAnnotation")
        default String getValue() {
            return "fromDefault";
        }
    }
    
    @Test
    public void annotationAndDefaultImplementationNotAllowed() throws ConfigException {
        try {
            Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                    }
                    
                    @Provides
                    @Singleton
                    public DefaultMethodWithAnnotation getMyConfig(ConfigProxyFactory factory) {
                        return factory.newProxy(DefaultMethodWithAnnotation.class);
                    }
                });
            
            injector.getInstance(DefaultMethodWithAnnotation.class);
            Assert.fail("Exepcted ProvisionException");
        } catch (ProvisionException e) {
            e.printStackTrace();
            Assert.assertEquals(IllegalArgumentException.class, e.getCause().getCause().getClass());
        } catch (Exception e) {
            Assert.fail("Expected ProvisionException");
        }
    }
}
