package netflix.archaius.property;

import netflix.archaius.RootConfig;
import netflix.archaius.Property;
import netflix.archaius.config.SimpleDynamicConfig;

import org.junit.Assert;
import org.junit.Test;

public class PropertyTest {
    public static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        
        public MyService(RootConfig manager) {
            value  = manager.observe("foo").asInteger(1, new MethodInvoker<Integer>(this, "setValue"));
            value2 = manager.observe("foo").asInteger(2);
        }
        
        public void setValue(Integer value) {
            System.out.println("Updating " + value);
        }
    }
    
    @Test
    public void test() {
        SimpleDynamicConfig config = new SimpleDynamicConfig("dyn");
        
        RootConfig manager = RootConfig.builder().build();
        manager.addConfig(config);
        
        MyService service = new MyService(manager);

        Assert.assertEquals(1, (int)service.value.get());
        Assert.assertEquals(2, (int)service.value2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, (int)service.value.get());
        Assert.assertEquals(123, (int)service.value2.get());
    }
}
