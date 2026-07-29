package com.netflix.archaius.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
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

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxyTest {
    public interface MyConfig {
        @DefaultValue("0")
        int getInteger();
        
        String getString();
        
        MySubConfig getSubConfig();
        
        default String getDefault() {
            return getInteger() + "-" + getString();
        }
    }
    
    public interface MySubConfig {
        @DefaultValue("0")
        int getInteger();
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
                @SuppressWarnings("unused")
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class);
                }
            }
        );
        
        SettableConfig cfg = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        MyConfig config = injector.getInstance(MyConfig.class);
        assertEquals("bar", config.getString());
        assertEquals(1, config.getInteger());
        assertEquals(2, config.getSubConfig().getInteger());
        assertEquals("1-bar", config.getDefault());
        cfg.setProperty("subConfig.integer", 3);
        
        assertEquals(3, config.getSubConfig().getInteger());
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
                @SuppressWarnings("unused")
                public MyConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(MyConfig.class, "prefix");
                }
            });
        
        MyConfig config = injector.getInstance(MyConfig.class);
        assertEquals("bar", config.getString());
        assertEquals(1, config.getInteger());
    }
    
    @Configuration(prefix="prefix-${env}", allowFields=true)
    @ConfigurationSource(value={"moduleTest"}, cascading=MyCascadingStrategy.class)
    public interface ModuleTestConfig {
        Boolean isLoaded();
        String getProp1();
    }
    
    @Test
    public void confirmConfigurationSourceWorksWithProxy() {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                @Provides
                @Singleton
                @SuppressWarnings("unused")
                public ModuleTestConfig getMyConfig(ConfigProxyFactory factory) {
                    return factory.newProxy(ModuleTestConfig.class, "moduleTest");
                }
            });
            
        ModuleTestConfig config = injector.getInstance(ModuleTestConfig.class);
        assertTrue(config.isLoaded());
        assertEquals("fromFile", config.getProp1());
        
        injector.getInstance(Config.class).accept(new PrintStreamVisitor());
    }
    
    public interface DefaultMethodWithAnnotation {
        @DefaultValue("fromAnnotation")
        @SuppressWarnings("unused")
        default String getValue() {
            return "fromDefault";
        }
    }
    
    @Test
    public void annotationAndDefaultImplementationNotAllowed() throws ConfigException {
        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Provides
                    @Singleton
                    @SuppressWarnings("unused")
                    public DefaultMethodWithAnnotation getMyConfig(ConfigProxyFactory factory) {
                        return factory.newProxy(DefaultMethodWithAnnotation.class);
                    }
                });

        ProvisionException pe = assertThrows(ProvisionException.class,
                () -> injector.getInstance(DefaultMethodWithAnnotation.class));

        Stream.iterate((Throwable) pe, t -> t != null ? t.getCause() : null)
                .limit(10) // avoid infinite loop
                .filter(t -> t instanceof IllegalArgumentException)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an IllegalArgumentException in the cause chain", pe));
    }
}
