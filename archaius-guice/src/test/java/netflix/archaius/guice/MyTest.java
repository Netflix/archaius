package netflix.archaius.guice;

import netflix.archaius.Config;
import netflix.archaius.DefaultConfigLoader;
import netflix.archaius.config.CascadingCompositeConfig;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MyTest {
    public static Config config;
    
    @BeforeClass
    public void setup() throws ConfigException {
        config = DefaultConfigLoader.builder().build().newLoader().load("application");
    }
    
    @Test
    public void test1() throws ConfigException {
        final CascadingCompositeConfig c = new CascadingCompositeConfig("ROOT");
        c.addConfigFirst(config);
        c.addConfigFirst(MapConfig.builder("overrides")
                .put("property.foo", true)
                .build());

        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Config.class).toInstance(c);
                    }
                },
                new ArchaiusModule()
                );

    }
    
    @Test
    public void test2() throws ConfigException {
        final CascadingCompositeConfig c = new CascadingCompositeConfig("ROOT");
        c.addConfigFirst(config);
        c.addConfigFirst(MapConfig.builder("overrides")
                .put("property.foo", false)
                .build());

        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Config.class).toInstance(c);
                    }
                },
                new ArchaiusModule()
                );

    }
}
