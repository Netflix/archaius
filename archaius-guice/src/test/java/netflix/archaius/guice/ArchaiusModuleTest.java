package netflix.archaius.guice;

import java.util.Properties;

import javax.inject.Inject;

import netflix.archaius.AppConfig;
import netflix.archaius.Config;
import netflix.archaius.Property;
import netflix.archaius.guice.annotations.Configuration;
import netflix.archaius.guice.annotations.ConfigurationSource;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class ArchaiusModuleTest {
    @Singleton
    @Configuration(prefix="prefix-${env}")
    @ConfigurationSource({"libA"})
    public static class MyServiceConfig {
        private String  str_value;
        private Integer int_value;
        private Boolean bool_value;
        private Double  double_value;
        
        private Property<Integer> fast_int;
        
        public void setStr_value(String value) {
            System.out.println("Setting string value to : " + value);
        }
        
        public void setInt_value(Integer value) {
            System.out.println("Setting int value to : " + value);
        }
        
        @Inject
        public MyServiceConfig() {
            
        }
    }
    
    @Singleton
    public static class MyService {
        private Boolean value;
        
        @Inject
        public MyService(AppConfig config, MyServiceConfig serviceConfig) {
            value = config.getBoolean("libA.loaded");
        }
        
        public Boolean getValue() {
            return value;
        }
    }
    
    @Test
    public void test() {
        Injector injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    Properties props = new Properties();
                    props.setProperty("prefix-prod.str_value", "str_value");
                    props.setProperty("prefix-prod.int_value", "123");
                    props.setProperty("prefix-prod.bool_value", "true");
                    props.setProperty("prefix-prod.double_value", "456.0");
                    props.setProperty("env", "prod");
                    
                    AppConfig config = AppConfig.builder().withProperties(props).build();
                    bind(Config.class).toInstance(config);
                    bind(AppConfig.class).toInstance(config);
                }
            },
            new ArchaiusModule()
            );
        
        MyService service = injector.getInstance(MyService.class);
        Assert.assertTrue(service.getValue());
        
        MyServiceConfig config = injector.getInstance(MyServiceConfig.class);
        Assert.assertEquals("str_value", config.str_value);
        Assert.assertEquals(123,   config.int_value.intValue());
        Assert.assertEquals(true,  config.bool_value);
        Assert.assertEquals(456.0, config.double_value, 0);
    }
}
