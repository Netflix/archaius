package com.netflix.config;

import com.netflix.config.sources.TypesafeConfigurationSource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TypesafePollingSourceTest
{
    private TestableTypesafeConfigurationSource source = new TestableTypesafeConfigurationSource();

    @Test
    public void archaiusSeesTypesafeUpdates() throws InterruptedException {
        DynamicPropertyFactory props = props();
        assertThat(props.getIntProperty("top", -1).get(), equalTo(3));

        source.setConf("reference-test-updated");
        Thread.sleep(200);

        assertThat(props.getIntProperty("top", -1).get(), equalTo(7));
    }

    private DynamicPropertyFactory props() {
        source.setConf("reference-test");

        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 10, false);
        DynamicConfiguration configuration = new DynamicConfiguration(source, scheduler);
        DynamicPropertyFactory.initWithConfigurationSource(configuration);

        return DynamicPropertyFactory.getInstance();
    }

    private static class TestableTypesafeConfigurationSource extends TypesafeConfigurationSource
    {
        private String conf;

        private void setConf(String conf) {
            this.conf = conf;
        }

        @Override
        protected Config config() {
            return ConfigFactory.load(conf);
        }
    }
}