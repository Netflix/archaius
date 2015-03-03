package netflix.archaius;

import netflix.archaius.cascade.ConcatCascadeStrategy;
import netflix.archaius.config.EnvironmentConfig;
import netflix.archaius.config.MapConfig;
import netflix.archaius.config.SimpleDynamicConfig;
import netflix.archaius.config.SystemConfig;
import netflix.archaius.loaders.PropertiesConfigLoader;
import netflix.archaius.property.DefaultPropertyObserver;

import org.junit.Test;

public class ConfigManagerTest {
    @Test
    public void testBasicReplacement() {
        SimpleDynamicConfig dyn = new SimpleDynamicConfig("FAST");
        
        RootConfig config = RootConfig.builder()
                .build();
        
        config.addConfigLast(dyn)
              .addConfigLast(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .put("c",      123)
                        .build())
              .addConfigLast(new EnvironmentConfig())
              .addConfigLast(new SystemConfig());
        
        Property<String> prop = config.observe("abc").asString("defaultValue");
        
        config.observe("abc").asString("defaultValue", new DefaultPropertyObserver<String>() {
            @Override
            public void onChange(String next) {
                System.out.println("Configuration changed : " + next);
            }
        });
        
        dyn.setProperty("abc", "${c}");
    }
    
    @Test
    public void testDefaultConfiguration() {
        RootConfig config = RootConfig.builder()
                .build();
        
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
                .withConfigLoader(new PropertiesConfigLoader())
                .build();
                
        config
            .addConfigLast(MapConfig.builder("test")
                    .put("env",    "prod")
                    .put("region", "us-east")
                    .build())
            .addConfigLast(loader.newLoader().load("application"));

        String str = config.getString("application.prop1");
        System.out.println(str);
    }
}
