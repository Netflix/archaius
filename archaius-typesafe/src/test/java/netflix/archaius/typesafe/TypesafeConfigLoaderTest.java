package netflix.archaius.typesafe;

import netflix.archaius.DefaultConfigLoader;
import netflix.archaius.DefaultAppConfig;
import netflix.archaius.cascade.ConcatCascadeStrategy;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigException;

import org.junit.Assert;
import org.junit.Test;

public class TypesafeConfigLoaderTest {
    @Test
    public void test() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder().build();
                
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withConfigLoader(new TypesafeConfigReader())
                .withStrInterpolator(config.getStrInterpolator())
                .build();
        
        config.addConfigLast(MapConfig.builder("test")
                        .put("env",    "prod")
                        .put("region", "us-east")
                        .build());
        
        config.addConfigLast(loader.newLoader()
              .withCascadeStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
              .load("foo"));
        
        
        Assert.assertEquals("foo-prod", config.getString("foo.prop1"));
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }   
}
