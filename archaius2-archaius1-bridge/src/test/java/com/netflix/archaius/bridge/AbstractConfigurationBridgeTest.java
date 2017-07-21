package com.netflix.archaius.bridge;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.visitor.PrintStreamVisitor;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;

@Ignore
public class AbstractConfigurationBridgeTest {
    @Singleton
    public static class SomeClient {
        final String fooValue;
        
        @Inject
        public SomeClient(Config config) {
            config.accept(new PrintStreamVisitor());
            fooValue = ConfigurationManager.getConfigInstance().getString("foo", null);
        }
    }
    
    public static class TestModule extends AbstractModule {
        private Properties properties;
        TestModule(Properties props) {
            this.properties = props;
        }
        
        TestModule() {
            this.properties = new Properties();
        }
        
        @Override
        protected void configure() {
            install(new StaticArchaiusBridgeModule());
            install(new ArchaiusModule());
            
            bind(SomeClient.class).asEagerSingleton();
        }
    }
    
    @Before
    public void before() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        StaticAbstractConfiguration.reset();
        ConfigBasedDeploymentContext.reset();
    }
    
    @Test
    public void testBasicWiring() {
        final Properties props = new Properties();
        props.setProperty("foo", "bar");
        
        Injector injector = Guice.createInjector(
            new TestModule(props),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(SomeClient.class).asEagerSingleton();
                }
            });
        
        SomeClient client = injector.getInstance(SomeClient.class);
        Assert.assertNotNull(ConfigurationManager.getConfigInstance());
        Assert.assertEquals("bar", client.fooValue);
    }
    
    @ConfigurationSource(value={"libA"})
    static class LibA {
    }
    
    @ConfigurationSource(value={"libB"})
    static class LibB {
    }
    
    @Test
    public void confirmOverrideOrder() throws IOException {
        ConfigurationManager.getConfigInstance();
        Assert.assertFalse(ConfigurationManager.isConfigurationInstalled());
        Injector injector = Guice.createInjector(new TestModule());
        
        Config config = injector.getInstance(Config.class);
        
        injector.getInstance(LibA.class);
        Assert.assertTrue(config.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        injector.getInstance(LibB.class);
        Assert.assertTrue(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
    }
    
    @Test
    public void basicBridgeTest() throws IOException {
        ConfigurationManager.getConfigInstance();
        DeploymentContext context1 = ConfigurationManager.getDeploymentContext();
        Assert.assertNotNull(context1);
        Assert.assertEquals(null, context1.getDeploymentEnvironment());
        
        Injector injector = Guice.createInjector(new TestModule());

        AbstractConfiguration config1 = ConfigurationManager.getConfigInstance();
        DeploymentContext contextDi = injector.getInstance(DeploymentContext.class);
        Assert.assertNotSame(contextDi, context1);
        ConfigurationManager.loadCascadedPropertiesFromResources("libA");
        Assert.assertTrue(config1.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config1.getString("lib.override", null));
        
        Config config2 = injector.getInstance(Config.class);
        SettableConfig settable = injector.getInstance(Key.get(SettableConfig.class, RuntimeLayer.class));
        settable.setProperty("@environment", "foo");
        
        DeploymentContext context2 = ConfigurationManager.getDeploymentContext();
        
        Assert.assertEquals("foo", ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
        Assert.assertEquals("foo", context2.getDeploymentEnvironment());
        Assert.assertNotSame(contextDi, context1);
        Assert.assertEquals("foo", context1.getDeploymentEnvironment());
        
        Assert.assertTrue(config2.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config2.getString("lib.override", null));
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libB");
        Assert.assertTrue(config1.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config1.getString("lib.override", null));
        Assert.assertTrue(config2.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config2.getString("lib.override", null));
        
    }
    
    /**
     * This test was written to confirm the legacy API behavior.  It cannot be run 
     * with the other tests since the static state of ConfigurationManager cannot
     * be reset between tests.
     * @throws IOException
     */
    @Test
    public void testBridgePropertiesFromLegacyToNew() throws IOException {
        Injector injector = Guice.createInjector(new TestModule());
        
        AbstractConfiguration config1 = ConfigurationManager.getConfigInstance();
        Config                config2 = injector.getInstance(Config.class);
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libA");
        Assert.assertTrue(config1.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config1.getString("lib.override", null));
        Assert.assertTrue(config2.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config2.getString("lib.override", null));
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libB");
        Assert.assertTrue(config1.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config1.getString("lib.override", null));
        Assert.assertTrue(config2.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config2.getString("lib.override", null));

    }

    /**
     * This test was written to confirm the legacy API behavior.  It cannot be run 
     * with the other tests since the static state of ConfigurationManager cannot
     * be reset between tests.
     * @throws IOException
     */
    @Test
    public void confirmLegacyOverrideOrder() throws IOException {
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libA");
        Assert.assertTrue(config.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libB");
        Assert.assertTrue(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        ConfigurationManager.loadCascadedPropertiesFromResources("libB");
        
        System.out.println(ConfigurationManager.getLoadedPropertiesURLs());
    }
    
    /**
     * This test was written to confirm the legacy API behavior.  It cannot be run 
     * with the other tests since the static state of ConfigurationManager cannot
     * be reset between tests.
     * @throws IOException
     */
    @Test
    public void confirmLegacyOverrideOrderResources() throws IOException {
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        
        ConfigurationManager.loadPropertiesFromResources("libA.properties");
        Assert.assertTrue(config.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        ConfigurationManager.loadPropertiesFromResources("libB.properties");
        Assert.assertTrue(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        System.out.println(ConfigurationManager.getLoadedPropertiesURLs());
    }
    
    /**
     * This test was written to confirm the legacy API behavior.  It cannot be run 
     * with the other tests since the static state of ConfigurationManager cannot
     * be reset between tests.
     * @throws IOException
     */
    @Test
    public void confirmLegacyOverrideOrderAddConfig() throws IOException {
        AggregatedConfiguration config = (AggregatedConfiguration) ConfigurationManager.getConfigInstance();
        
        Properties p1 = new Properties();
        p1.setProperty("lib.override", "libA");
        p1.setProperty("libA.loaded", "true");
        config.addConfiguration(new MapConfiguration(p1));
        
        Assert.assertTrue(config.getBoolean("libA.loaded",  false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
        
        Properties p2 = new Properties();
        p2.setProperty("lib.override", "libB");
        p2.setProperty("libB.loaded", "true");
        config.addConfiguration(new MapConfiguration(p2));
        
        Assert.assertTrue(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("lib.override", null));
    }
}
