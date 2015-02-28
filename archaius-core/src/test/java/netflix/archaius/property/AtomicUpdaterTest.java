package netflix.archaius.property;

import java.util.concurrent.atomic.AtomicInteger;

import netflix.archaius.ConfigManager;
import netflix.archaius.config.SimpleDynamicConfig;

import org.junit.Assert;
import org.junit.Test;

public class AtomicUpdaterTest {
    public static class MyService {
        private AtomicInteger value = new AtomicInteger(1);
        private AtomicInteger value2 = new AtomicInteger(2);
        private ConfigManager manager;
        
        public MyService(ConfigManager manager) {
            this.manager = manager;
        }
        
        public void init() {
            manager.listen("foo").subscribe(new MethodInvoker<Integer>(this, "setValue", 3));
            manager.listen("foo").subscribe(new AtomicIntegerUpdater(value2));
        }
        
        public void setValue(Integer value) {
            System.out.println("Updating " + value);
            this.value.set(value);
        }
    }
    
    @Test
    public void test() {
        SimpleDynamicConfig config = new SimpleDynamicConfig("dyn");
        
        ConfigManager manager = ConfigManager.builder().build();
        manager.addConfig(config);
        
        MyService service = new MyService(manager);

        Assert.assertEquals(1, service.value.get());
        Assert.assertEquals(2, service.value2.get());
        
        service.init();
        
        Assert.assertEquals(3, service.value.get());
        Assert.assertEquals(2, service.value2.get());
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals(123, service.value.get());
        Assert.assertEquals(123, service.value2.get());
    }
}
