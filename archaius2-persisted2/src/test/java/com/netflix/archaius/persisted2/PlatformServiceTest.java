package com.netflix.archaius.persisted2;

import javax.inject.Singleton;

import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;
import com.netflix.archaius.Config;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.inject.RemoteLayer;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class PlatformServiceTest {
    // TODO: Provide an embedded version of this service.  For now these tests are run
    // manually against internal Netflix systems
    @Test
    @Ignore
    public void test() {
        final Persisted2ClientConfig config = new DefaultPersisted2ClientConfig()
            .withServiceUrl("http://platformservice.us-east-1.dyntest.netflix.net:7001/platformservice/REST/v2/properties/jsonFilterprops")
            .withQueryScope("env",    "test")
            .withQueryScope("region", "us-east-1")
            .withQueryScope("appId",  "NCCP")
            .withScope("env",    "test")
            .withScope("region", "us-east-1")
            .withScope("appId",    "NCCP")
            .withPrioritizedScopes("env", "region", "asg", "stack", "serverId")
//            .withSkipPropsWithExtraScopes(true)
            ;

        Injector injector = Guice.createInjector(Modules.override(new ArchaiusModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Persisted2ClientConfig.class).toInstance(config);
                bind(Config.class).annotatedWith(RemoteLayer.class).toProvider(Persisted2ConfigProvider.class).in(Scopes.SINGLETON);
            }
            
            @Provides
            @Singleton
            Config getConfig(@RemoteLayer Config config) {
                return config;
            }
        }));
        
        Config c = injector.getInstance(Config.class);
        c.accept(new PrintStreamVisitor());
    }
}
