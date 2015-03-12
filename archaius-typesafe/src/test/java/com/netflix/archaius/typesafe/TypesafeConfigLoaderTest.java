package com.netflix.archaius.typesafe;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class TypesafeConfigLoaderTest {
    @Test
    public void test() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withApplicationConfigName("application")
                .build();
                
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withConfigReader(new TypesafeConfigReader())
                .withStrInterpolator(config.getStrInterpolator())
                .build();
        
        config.addConfigLast(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .build());
        
        config.addConfigLast(loader.newLoader()
              .withCascadeStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
              .load("foo"));
        
        
        Assert.assertEquals("foo-prod", config.getString("foo.prop1"));
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }   
}
