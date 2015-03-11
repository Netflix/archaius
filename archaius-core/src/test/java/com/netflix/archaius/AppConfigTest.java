package com.netflix.archaius;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class AppConfigTest {
    @Test
    public void testAppAndLibraryLoading() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");
        
        DefaultAppConfig config = DefaultAppConfig.builder()
            .withApplicationConfigName("application")
            .withProperties(props)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        System.out.println(config);
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        config.addConfigLast(config.newLoader().load("libA"));
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        config.addConfigFirst(config.newLoader().load("libB"));
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertTrue(config.getBoolean("libB.loaded"));
        Assert.assertEquals("libB", config.getString("libA.overrideA"));
        
        config.accept(new PrintStreamVisitor());
    }
}
