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
            value  = config.observeProperty("foo").asInteger();
            value.addObserver(new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.observeProperty("foo").asInteger();
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

        Assert.assertEquals(1, (int)service.value.get(1));
        Assert.assertEquals(2, (int)service.value2.get(2));
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, (int)service.value.get(1));
        Assert.assertEquals(123, (int)service.value2.get(2));
    }
    
    @Test
    public void testPropertyIsCached() throws ConfigException {
        DefaultAppConfig config = DefaultAppConfig.builder().withApplicationConfigName("application").build();
        
        System.out.println("Configs: " + config.getChildConfigNames());
        
        Property<Integer> intProp1 = config.observeProperty("foo").asInteger();
        Property<Integer> intProp2 = config.observeProperty("foo").asInteger();
        Property<String>  strProp  = config.observeProperty("foo").asString();

        Assert.assertSame(intProp1, intProp2);
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get(null));
        Assert.assertEquals((Integer)123, intProp1.get(null));
        Assert.assertEquals((Integer)123, intProp2.get(null));
    }

}
