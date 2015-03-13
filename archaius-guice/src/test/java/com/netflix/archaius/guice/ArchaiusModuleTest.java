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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.netflix.archaius.AppConfig;
import com.netflix.archaius.Config;
import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.Property;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.MappingException;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.mapper.DefaultConfigMapper;
import com.netflix.archaius.mapper.annotations.Configuration;
import com.netflix.archaius.mapper.annotations.ConfigurationSource;
import com.netflix.archaius.mapper.annotations.DefaultValue;

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
        public MyService(AppConfig config, MyServiceConfig serviceConfig) {
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
    public void test() {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                protected AppConfig createAppConfig() {
                    Properties props = new Properties();
                    props.setProperty("prefix-prod.str_value", "str_value");
                    props.setProperty("prefix-prod.int_value", "123");
                    props.setProperty("prefix-prod.bool_value", "true");
                    props.setProperty("prefix-prod.double_value", "456.0");
                    props.setProperty("env", "prod");
                    
                    return DefaultAppConfig.builder().withProperties(props).build();
                }
            }
        );
        
        MyService service = injector.getInstance(MyService.class);
        Assert.assertTrue(service.getValue());
        
        MyServiceConfig serviceConfig = injector.getInstance(MyServiceConfig.class);
        Assert.assertEquals("str_value", serviceConfig.str_value);
        Assert.assertEquals(123,   serviceConfig.int_value.intValue());
        Assert.assertEquals(true,  serviceConfig.bool_value);
        Assert.assertEquals(456.0, serviceConfig.double_value, 0);

        Config config = injector.getInstance(Config.class);
        
        Assert.assertTrue(config.getBoolean("moduleTest.loaded"));
        Assert.assertTrue(config.getBoolean("moduleTest-prod.loaded"));
    }
    
    @Test
    public void testNamedInjection() {
        Injector injector = Guice.createInjector(
            new ArchaiusModule() {
                protected void configure() {
                    super.configure();
                    
                    bind(Named.class).annotatedWith(Names.named("name1")).to(Named1.class);
                    bind(Named.class).annotatedWith(Names.named("name2")).to(Named2.class);
                }
                
                protected AppConfig createAppConfig() {
                    Properties props = new Properties();
                    props.setProperty("prefix-prod.named", "name1");
                    props.setProperty("env", "prod");
                    
                    return DefaultAppConfig.builder().withProperties(props).build();
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
        Config config = MapConfig.builder("")
                .put("prefix.foo.123.loaded", "loaded")
                .build();
        
        DefaultConfigMapper binder = new DefaultConfigMapper();
        
        ChildService service = new ChildService("foo", 123L);
        binder.mapConfig(service, config);
        Assert.assertEquals("loaded", service.loaded);
    }
    
    public static interface TestProxyConfig {
        @DefaultValue("default")
        String getString();
    }
    
    @Test
    public void testProxy() {
        Injector injector = Guice.createInjector(
                new ArchaiusModule(),
                ArchaiusModule.forProxy(TestProxyConfig.class)
            );
        
        AppConfig appConfig = injector.getInstance(AppConfig.class);
        
        TestProxyConfig config = injector.getInstance(TestProxyConfig.class);
        Assert.assertEquals("default", config.getString());
        
        appConfig.setProperty("string", "new");
        Assert.assertEquals("new", config.getString());
        
        appConfig.clearProperty("string");
        Assert.assertEquals("default", config.getString());
    }
}
