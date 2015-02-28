package netflix.archaius.typesafe;

import netflix.archaius.ConfigManager;
import netflix.archaius.cascade.ConcatCascadeStrategy;
import netflix.archaius.config.MapConfig;

import org.junit.Assert;
import org.junit.Test;

public class TypesafeConfigLoaderTest {
    @Test
    public void test() {
        ConfigManager config = ConfigManager.builder()
                .withConfigLoader(new TypesafeConfigLoader())
                .build();
        
        config.addConfig(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .build());
        
        config.addConfig(config.newLoader()
              .withCascadeStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
              .load("foo"));
        
        
        Assert.assertEquals("foo-prod", config.getString("foo.prop1"));
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }   
}
