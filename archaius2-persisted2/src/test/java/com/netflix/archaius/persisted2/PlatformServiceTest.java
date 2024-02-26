package com.netflix.archaius.persisted2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.visitor.PrintStreamVisitor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class PlatformServiceTest {
    // TODO: Provide an embedded version of this service.  For now these tests are run
    // manually against internal Netflix systems
    @Test
    @Disabled
    public void test() throws ConfigException {
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

        Injector injector = Guice.createInjector(
                new ArchaiusModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Persisted2ClientConfig.class).toInstance(config);
                        bind(Config.class).annotatedWith(RemoteLayer.class).toProvider(Persisted2ConfigProvider.class).in(Scopes.SINGLETON);
                    }
                });
        
        Config c = injector.getInstance(Config.class);
        c.accept(new PrintStreamVisitor());
    }
}
