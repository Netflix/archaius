/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.guice;

import java.util.Properties;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.netflix.archaius.ConfigMapper;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.MappingException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class ArchaiusModuleTest {
    
    public static class MyCascadingStrategy extends ConcatCascadeStrategy {
        public MyCascadingStrategy() {
            super(new String[]{"${env}"});
        }
    }
    
    @Singleton
    @Configuration(prefix="prefix-${env}", allowFields=true)
    @ConfigurationSource(value={"moduleTest"}, cascading=MyCascadingStrategy.class)
    public static class MyServiceConfig {
        private String  str_value;
        private Integer int_value;
        private Boolean bool_value;
        private Double  double_value;
        private Property<Integer> fast_int;
        private Named named;
        
        public void setStr_value(String value) {
            System.out.println("Setting string value to : " + value);
        }
        
        public void setInt_value(Integer value) {
            System.out.println("Setting int value to : " + value);
        }
        
        public void setNamed(Named named) {
            this.named = named;
        }
        
        @Inject
        public MyServiceConfig() {
            
        }
    }
    
    @Singleton
    public static class MyService {
        private Boolean value;
        
        @Inject
        public MyService(Config config, MyServiceConfig serviceConfig) {
            value = config.getBoolean("moduleTest.loaded");
        }
        
        public Boolean getValue() {
            return value;
        }
    }
    
    public static interface Named {
        
    }
    
    @Singleton
    public static class Named1 implements Named {
        
    }
    
    @Singleton
    public static class Named2 implements Named {
        
    }
    
    @Test
    public void test()  {
        final Properties props = new Properties();
        props.setProperty("prefix-prod.str_value", "str_value");
        props.setProperty("prefix-prod.int_value", "123");
        props.setProperty("prefix-prod.bool_value", "true");
        props.setProperty("prefix-prod.double_value", "456.0");
        props.setProperty("env", "prod");
        
        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bindApplicationConfigurationOverride().toInstance(MapConfig.from(props));
                    }
                });
        
        Config config = injector.getInstance(Config.class);
        Assert.assertEquals("prod", config.getString("env"));
        
        config.accept(new PrintStreamVisitor(System.err));
        
        MyService service = injector.getInstance(MyService.class);
        Assert.assertTrue(service.getValue());
        
        MyServiceConfig serviceConfig = injector.getInstance(MyServiceConfig.class);
        Assert.assertEquals("str_value", serviceConfig.str_value);
        Assert.assertEquals(123,   serviceConfig.int_value.intValue());
        Assert.assertEquals(true,  serviceConfig.bool_value);
        Assert.assertEquals(456.0, serviceConfig.double_value, 0);
        
        Assert.assertTrue(config.getBoolean("moduleTest.loaded"));
        Assert.assertTrue(config.getBoolean("moduleTest-prod.loaded"));
    }
    
    @Test
    public void testNamedInjection()  {
        final Properties props = new Properties();
        props.setProperty("prefix-prod.named", "name1");
        props.setProperty("env", "prod");
        
        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bindApplicationConfigurationOverride().toInstance(MapConfig.from(props));
                    }
                },
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Named.class).annotatedWith(Names.named("name1")).to(Named1.class);
                        bind(Named.class).annotatedWith(Names.named("name2")).to(Named2.class);
                    }
                }
            );
            
        MyService service = injector.getInstance(MyService.class);
        Assert.assertTrue(service.getValue());
        
        MyServiceConfig serviceConfig = injector.getInstance(MyServiceConfig.class);

        Assert.assertTrue(serviceConfig.named instanceof Named1);
    }

    @Configuration(prefix="prefix.${name}.${id}", params={"name", "id"}, allowFields=true)
    public static class ChildService {
        private final String name;
        private final Long id;
        private String loaded;
        
        public ChildService(String name, Long id) {
            this.name = name;
            this.id = id;
        }
    }
    
    @Test
    public void testPrefixReplacements() throws MappingException {
        Config config = MapConfig.builder()
                .put("prefix.foo.123.loaded", "loaded")
                .build();
        
        ConfigMapper binder = new ConfigMapper();
        
        ChildService service = new ChildService("foo", 123L);
        binder.mapConfig(service, config);
        Assert.assertEquals("loaded", service.loaded);
    }
    
    public static interface TestProxyConfig {
        @DefaultValue("default")
        String getString();
        
        @DefaultValue("foo,bar")
        String[] getStringArray();
        
        @DefaultValue("1,2")
        Integer[] getIntArray();
    }
    
    @Test
    public void testProxy()  {
        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Provides
                    @Singleton
                    public TestProxyConfig getProxyConfig(ConfigProxyFactory factory) {
                        return factory.newProxy(TestProxyConfig.class);
                    }
                }
            );
        
        Config config = injector.getInstance(Config.class);
        SettableConfig settableConfig = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        
        TestProxyConfig object = injector.getInstance(TestProxyConfig.class);
        Assert.assertEquals("default", object.getString());
        Assert.assertArrayEquals(new String[]{"foo", "bar"}, object.getStringArray());
        Assert.assertArrayEquals(new Integer[]{1,2}, object.getIntArray());
        
        settableConfig.setProperty("string", "new");
        settableConfig.setProperty("stringArray", "foonew,barnew");
        settableConfig.setProperty("intArray", "3,4");
        config.accept(new PrintStreamVisitor());
        
        Assert.assertEquals("new", object.getString());
        Assert.assertArrayEquals(new String[]{"foonew", "barnew"}, object.getStringArray());
        Assert.assertArrayEquals(new Integer[]{3,4}, object.getIntArray());
        
        settableConfig.clearProperty("string");
        Assert.assertEquals("default", object.getString());
    }
    
    @Test
    public void testDefaultBindings() {
        Injector injector = Guice.createInjector(
                new ArchaiusModule()
            );
        
        injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        injector.getInstance(Key.get(CompositeConfig.class, LibrariesLayer.class));
        injector.getInstance(Config.class);
    }
    
    @Test
    public void testApplicationOverrideLayer() {
        final Properties props = new Properties();
        props.setProperty("a", "override");
        
        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bindApplicationConfigurationOverride().toInstance(MapConfig.from(props));
                    }
                });
        
        Config config = injector.getInstance(Config.class);
        Assert.assertEquals("override", config.getString("a"));
    }
    
    @Test
    public void testBasicLibraryOverride() {
        final Properties props = new Properties();
        props.setProperty("moduleTest.prop1", "fromOverride");
        
        Injector injector = Guice.createInjector(new ArchaiusModule());
        
        injector.getInstance(MyServiceConfig.class);
        Config config = injector.getInstance(Config.class);
        Assert.assertEquals("fromFile", config.getString("moduleTest.prop1"));
    }
    
    @Test
    public void testLibraryOverride() {
        final Properties props = new Properties();
        props.setProperty("moduleTest.prop1", "fromOverride");
        
        Injector injector = Guice.createInjector(
              new ArchaiusModule() {
                  @Override
                  protected void configureArchaius() {
                      bindApplicationConfigurationOverride().toInstance(MapConfig.from(props));
                  }
              },
              new AbstractModule() {
                    @Override
                    protected void configure() {
                        MapBinder.newMapBinder(binder(), String.class, Config.class, LibrariesLayer.class)
                            .addBinding("moduleTest")
                            .toInstance(MapConfig.from(props));
                    }
                }
            );
        
        Config config = injector.getInstance(Config.class);
        injector.getInstance(MyServiceConfig.class);
        config.accept(new PrintStreamVisitor());
        Assert.assertEquals("fromOverride", config.getString("moduleTest.prop1"));
    }

    @Test
    public void succeedOnDuplicateInstall() {
        Guice.createInjector(
                new ArchaiusModule(),
                new ArchaiusModule());
    }
    
}
