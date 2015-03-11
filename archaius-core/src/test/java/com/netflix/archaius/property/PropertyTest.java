package com.netflix.archaius.property;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.Property;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.property.MethodInvoker;

public class PropertyTest {
    public static class MyService {
        private Property<Integer> value;
        private Property<Integer> value2;
        
        public MyService(DefaultAppConfig config) {
            value  = config.connectProperty("foo").asInteger().addObserver(new MethodInvoker<Integer>(this, "setValue"));
            value2 = config.connectProperty("foo").asInteger();
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
        
        Property<Integer> intProp1 = config.connectProperty("foo").asInteger();
        Property<Integer> intProp2 = config.connectProperty("foo").asInteger();
        Property<String>  strProp  = config.connectProperty("foo").asString();

        Assert.assertSame(intProp1, intProp2);
        
        config.setProperty("foo", "123");
        
        Assert.assertEquals("123", strProp.get(null));
        Assert.assertEquals((Integer)123, intProp1.get(null));
        Assert.assertEquals((Integer)123, intProp2.get(null));
    }

}
