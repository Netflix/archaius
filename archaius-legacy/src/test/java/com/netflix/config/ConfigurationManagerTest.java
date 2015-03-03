package com.netflix.config;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.netflix.config.DefaultLegacyConfigurationManager.Builder;
import com.netflix.config.DeploymentContext.ContextKey;

public class ConfigurationManagerTest {
    @Before
    public void beforeEachTest() {
        ConfigurationManager.clearInstance();
    }
    
    @Rule
    public TestName name = new TestName();
    
    @Test
    public void testDefaultStatic() {
        for (ContextKey key : ContextKey.values()) {
            System.setProperty(key.getKey(), key.name() + "-set");
        }
        
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        
        System.setProperty("someproperty", "prefix-${@environment}");
        
        Assert.assertEquals("prefix-environment-set", config.getString("someproperty"));
    }
    
    @Test 
    public void testAppOverride() {
        try {
            ConfigurationManager.loadAppOverrideProperties("test");
            
            AbstractConfiguration config = ConfigurationManager.getConfigInstance();
            Boolean value = config.getBoolean("testloaded", false);
            Assert.assertTrue(value);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test(expected=Exception.class)
    public void testAppOverrideNotFound() throws IOException {
        ConfigurationManager.loadAppOverrideProperties("notfound");
    }
    
    public static class CustomFactory implements LegacyRootConfigurationFactory {
        static AtomicInteger counter = new AtomicInteger();
        
        @Override
        public LegacyConfigurationManager create() throws Exception {
            counter.incrementAndGet();
            Builder builder = DefaultLegacyConfigurationManager.builder()
                    .withSystemProperties(false)
                    .withEnvironmentProperties(false)
                    ;
            
            return builder.build();
        }
    }
    
    @Test(expected=NoSuchElementException.class)
    public void testCustomFactory() {
        System.setProperty(name.getMethodName(), "true");
        
        CustomFactory.counter.set(0);
        System.setProperty("archaius.default.configuration.factory.class", CustomFactory.class.getName());
        
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        
        Assert.assertEquals(CustomFactory.counter.get(), 1);
        config.getBoolean(name.getMethodName());    // This will throw the NoSuchElementException
        Assert.fail("Should have failed with NoSuchElementException");
    }
}
