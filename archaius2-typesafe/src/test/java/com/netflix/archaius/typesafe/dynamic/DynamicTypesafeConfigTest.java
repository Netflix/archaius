package com.netflix.archaius.typesafe.dynamic;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class DynamicTypesafeConfigTest {

    @Test
    public void test() throws ConfigException {
        final TypesafeClientConfig config = new DefaultTypesafeClientConfig.Builder()
                .withConfigFilePath(getResourceUrl("dynamic/reference.conf"))
                .withRefreshIntervalMs(100)
                .build();

        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bind(TypesafeClientConfig.class).toInstance(config);
                        bind(Config.class)
                                .annotatedWith(RemoteLayer.class)
                                .toProvider(TypesafeConfigProvider.class)
                                .in(Scopes.SINGLETON);
                    }
                });

        SampleAppConfig appConfig = injector.getInstance(SampleAppConfig.class);

        assertEquals("app name", appConfig.name);
        assertEquals(true, appConfig.flag);
        assertEquals(111, appConfig.number.intValue());
        assertEquals(Lists.newArrayList(1, 2), appConfig.list);
    }

    @Test
    public void testDynamicConfigChange() throws ConfigException, InterruptedException {
        String[] configPath = new String[]{"dynamic/reference.conf"};

        final TypesafeClientConfig config = new TypesafeClientConfig() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public int getRefreshRateMs() {
                return 200;
            }

            @Override
            public String getTypesafeConfigPath() {
                return configPath[0];
            }

            @Override
            public Supplier<com.typesafe.config.Config> getTypesafeConfigSupplier() {
                return () -> {
                    String typesafeConfigPath = getTypesafeConfigPath();
                    System.out.println("Load from: " + typesafeConfigPath);
                    return ConfigFactory.load(typesafeConfigPath);
                };
            }
        };

        Injector injector = Guice.createInjector(
                new ArchaiusModule() {
                    @Override
                    protected void configureArchaius() {
                        bind(TypesafeClientConfig.class).toInstance(config);
                        bind(Config.class)
                                .annotatedWith(RemoteLayer.class)
                                .toProvider(TypesafeConfigProvider.class)
                                .in(Scopes.SINGLETON);
                    }
                });

        SampleAppConfig appConfig = injector.getInstance(SampleAppConfig.class);
        assertEquals("app name", appConfig.name);
        assertEquals(true, appConfig.flag);
        assertEquals(111, appConfig.number.intValue());
        assertEquals(Lists.newArrayList(1, 2), appConfig.list);

        // Simulate configuration update at runtime.
        configPath[0] = "dynamic/updated.conf";
        Thread.sleep(400);

        SampleAppConfig updatedAppConfig = injector.getInstance(SampleAppConfig.class);
        assertEquals("updated app name", updatedAppConfig.name);
        assertEquals(false, updatedAppConfig.flag);
        assertEquals(222, updatedAppConfig.number.intValue());
        assertEquals(Lists.newArrayList(3, 4), updatedAppConfig.list);
    }

    private String getResourceUrl(String resourceName) {
        return "file://" + getClass().getClassLoader().getResource(resourceName).getPath();
    }
}
