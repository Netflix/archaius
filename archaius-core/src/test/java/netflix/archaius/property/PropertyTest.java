package netflix.archaius.property;

import netflix.archaius.DefaultAppConfig;
import netflix.archaius.Property;
import netflix.archaius.exceptions.ConfigException;

import org.junit.Assert;
import org.junit.Test;

public class PropertyTest {
    public static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        
        public MyService(DefaultAppConfig config) {
            value  = config.createProperty("foo").asInteger(1, new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.createProperty("foo").asInteger(2);
        }
        
        public void setValue(Integer value) {
            System.out.println("Updating " + value);
        }
    }
    
    @Test
    public void test() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder().withApplicationConfigName("application").build();
        
        System.out.println("Configs: " + config.getChildConfigNames());
        
        MyService service = new MyService(config);

        Assert.assertEquals(1, (int)service.value.get());
        Assert.assertEquals(2, (int)service.value2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, (int)service.value.get());
        Assert.assertEquals(123, (int)service.value2.get());
    }

}
