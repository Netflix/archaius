package com.netflix.archaius;

import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.Property;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.config.SimpleDynamicConfig;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.loaders.PropertiesConfigReader;
import com.netflix.archaius.property.DefaultPropertyObserver;

public class ConfigManagerTest {
    @Test
    public void testBasicReplacement() throws ConfigException {
        SimpleDynamicConfig dyn = new SimpleDynamicConfig("FAST");
        
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withApplicationConfigName("application")
                .build();
        
        config.addConfigLast(dyn);
        config.addConfigLast(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .put("c",      123)
                        .build());
        config.addConfigLast(new EnvironmentConfig());
        config.addConfigLast(new SystemConfig());
        
        Property<String> prop = config.connectProperty("abc").asString();
        
        prop.addObserver(new DefaultPropertyObserver<String>() {
            @Override
            public void onChange(String next) {
                System.out.println("Configuration changed : " + next);
            }
        });
        
        dyn.setProperty("abc", "${c}");
    }
    
    @Test
    public void testDefaultConfiguration() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withApplicationConfigName("application")
                .build();
        
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
                .withConfigReader(new PropertiesConfigReader())
                .build();
                
        config.addConfigLast(MapConfig.builder("test")
                    .put("env",    "prod")
                    .put("region", "us-east")
                    .build());

        String str = config.getString("application.prop1");
        System.out.println(str);
    }
}
