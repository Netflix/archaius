package com.netflix.archaius.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.visitor.PrintStreamVisitor;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationInjectingListenerTest {
    
    @ConfigurationSource({"moduleTest", "moduleTest-prod"}) 
    public static class Foo {
        
    }
    
    @Test
    public void confirmLoadOrder() {
        Injector injector = Guice.createInjector(new ArchaiusModule());
        injector.getInstance(Foo.class);
        
        Config config = injector.getInstance(Config.class);
        config.accept(new PrintStreamVisitor());
        Assert.assertEquals("prod", config.getString("moduleTest.value"));
    }

    public static class Bar {

    }

    public static class BarModule extends AbstractModule {

        @Override
        protected void configure() {

        }

        @ConfigurationSource({"moduleTest"})
        @Provides
        @Singleton
        public Bar getBar() {
            return new Bar();
        }
    }

    @Test
    public void configProvidesMethodConfigLoaded() {
        Injector injector = Guice.createInjector(new ArchaiusModule(), new BarModule());
        injector.getInstance(Bar.class);

        Config config = injector.getInstance(Config.class);
        config.accept(new PrintStreamVisitor());
        Assert.assertEquals("true", config.getString("moduleTest.loaded"));
    }

}
