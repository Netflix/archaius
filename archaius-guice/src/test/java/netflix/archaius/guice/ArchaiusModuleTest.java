package netflix.archaius.guice;

import java.util.Properties;

import javax.inject.Inject;

import netflix.archaius.AppConfig;
import netflix.archaius.Config;
import netflix.archaius.DefaultAppConfig;
import netflix.archaius.Property;
import netflix.archaius.cascade.ConcatCascadeStrategy;
import netflix.archaius.mapper.annotations.Configuration;
import netflix.archaius.mapper.annotations.ConfigurationSource;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class ArchaiusModuleTest {
    
    public static class MyCascadingStrategy extends ConcatCascadeStrategy {
        public MyCascadingStrategy() {
            super(new String[]{"${env}"});
        }
    }
    
    @Singleton
    @Configuration(prefix="prefix-${env}")
    @ConfigurationSource(value={"moduleTest"}, cascading=MyCascadingStrategy.class)
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
            value = config.getBoolean("moduleTest.loaded");
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
                    
                    AppConfig config = DefaultAppConfig.builder().withProperties(props).build();
                    bind(Config.class).toInstance(config);
                    bind(AppConfig.class).toInstance(config);
                }
            },
            new ArchaiusModule()
            );
        
        MyService service = injector.getInstance(MyService.class);
        Assert.assertTrue(service.getValue());
        
        MyServiceConfig serviceConfig = injector.getInstance(MyServiceConfig.class);
        Assert.assertEquals("str_value", serviceConfig.str_value);
        Assert.assertEquals(123,   serviceConfig.int_value.intValue());
        Assert.assertEquals(true,  serviceConfig.bool_value);
        Assert.assertEquals(456.0, serviceConfig.double_value, 0);

        Config config = injector.getInstance(Config.class);
        
        Assert.assertTrue(config.getBoolean("moduleTest.loaded"));
        Assert.assertTrue(config.getBoolean("moduleTest-prod.loaded"));
    }
}
