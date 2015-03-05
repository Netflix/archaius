package netflix.archaius;

import netflix.archaius.cascade.ConcatCascadeStrategy;
import netflix.archaius.config.EnvironmentConfig;
import netflix.archaius.config.MapConfig;
import netflix.archaius.config.SimpleDynamicConfig;
import netflix.archaius.config.SystemConfig;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.loaders.PropertiesConfigLoader;
import netflix.archaius.property.DefaultPropertyObserver;

import org.junit.Test;

public class ConfigManagerTest {
    @Test
    public void testBasicReplacement() throws ConfigException {
        SimpleDynamicConfig dyn = new SimpleDynamicConfig("FAST");
        
        AppConfig config = AppConfig.builder()
                .withApplicationConfigName("application")
                .build();
        
        config.addConfigLast(dyn);
        config.addConfigLast(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .put("c",      123)
                        .build());
        config.addConfigLast(new EnvironmentConfig());
        config.addConfigLast(new SystemConfig());
        
        Property<String> prop = config.createProperty("abc").asString("defaultValue");
        
        config.createProperty("abc").asString("defaultValue", new DefaultPropertyObserver<String>() {
            @Override
            public void onChange(String next) {
                System.out.println("Configuration changed : " + next);
            }
        });
        
        dyn.setProperty("abc", "${c}");
    }
    
    @Test
    public void testDefaultConfiguration() throws ConfigException {
        AppConfig config = AppConfig.builder()
                .withApplicationConfigName("application")
                .build();
        
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
                .withConfigLoader(new PropertiesConfigLoader())
                .build();
                
        config.addConfigLast(MapConfig.builder("test")
                    .put("env",    "prod")
                    .put("region", "us-east")
                    .build());

        String str = config.getString("application.prop1");
        System.out.println(str);
    }
}
