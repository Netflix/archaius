package com.netflix.config;

import java.util.Properties;
import java.util.function.Function;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;

public class Archaius2PropertyRepoTest extends AbstractPropertyRepoTest<Archaius2PropertyRepo> {
    static class TestFixture implements Function<Properties, Archaius2PropertyRepo> {
        private SettableConfig mutableConfig;

        public Archaius2PropertyRepo apply(Properties properties) {
            this.mutableConfig = new DefaultSettableConfig();
            this.mutableConfig.setProperties(properties);
            return new Archaius2PropertyRepo(new DefaultPropertyFactory(mutableConfig));
        }
    }

    static TestFixture testFixture = new TestFixture();

    public Archaius2PropertyRepoTest() {
        super(testFixture, (k, v) -> {
            testFixture.mutableConfig.setProperty(k, v);
        });
    }

}
