package com.netflix.archaius.bridge;

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.archaius.Config;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.inject.LibrariesLayer;
import com.netflix.archaius.inject.RuntimeLayer;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.util.ConfigurationUtils;

public class AbstractConfigurationBridgeTest {
    @Singleton
    public static class SomeClient {
        final String fooValue;
        
        public SomeClient() {
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
            install(Modules
                    .override(new ArchaiusModule())
                    .with(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(SomeClient.class).asEagerSingleton();
                            bind(Properties.class).annotatedWith(RuntimeLayer.class).toInstance(properties);
                        }
                        
                        @Provides
                        @Singleton
                        Config getConfig(@LibrariesLayer CompositeConfig libraries, @RuntimeLayer SettableConfig settable) throws ConfigException {
                            return CompositeConfig.builder()
                                    .withConfig("runtime", settable)
                                    .withConfig("lib", libraries)
                                    .build()
                                    ;
                        }
                    }));
            install(new StaticArchaiusBridgeModule());
        }
    }
    
    @Before
    public void before() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        StaticAbstractConfiguration.reset();
        StaticDeploymentContext.reset();
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
    
    @Test
    public void testLoadProperties() {
        Injector injector = Guice.createInjector(new TestModule());
        
        AbstractConfiguration oldConfig = ConfigurationManager.getConfigInstance();
        Config config = injector.getInstance(Config.class);
        
        Assert.assertNull(config.getString("foo", null));
        Assert.assertNull(oldConfig.getString("foo", null));
        
        final Properties props = new Properties();
        props.setProperty("foo", "bar");
       
        ConfigurationUtils.loadProperties(props, oldConfig);
        
        Assert.assertEquals("bar", config.getString("foo", null));
        Assert.assertEquals("bar", oldConfig.getString("foo", null));
    }


}
