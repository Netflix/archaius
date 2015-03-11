package netflix.archaius.mapper;

import java.util.Properties;

import netflix.archaius.DefaultAppConfig;
import netflix.archaius.mapper.annotations.DefaultValue;
import netflix.archaius.property.PrefixedObservablePropertyFactory;

import org.junit.Assert;
import org.junit.Test;

public class DefaultConfigMapperTest {
    public static interface MyConfig {
        @DefaultValue("notoaded")
        String getString();
        
        @DefaultValue("123")
        int getInteger();
        
        Integer getInteger2();
        
        @DefaultValue("true")
        boolean getBoolean();
        
        Boolean getBoolean2();
        
        @DefaultValue("3")
        short getShort();
        
        Short getShort2();
        
        @DefaultValue("3")
        long getLong();
        
        Long getLong2();

        @DefaultValue("3.1")
        float getFloat();
        
        Float getFloat2();
        
        @DefaultValue("3.1")
        double getDouble();
        
        Double getDouble2();
    }
    
    @Test
    public void testProxy() {
        Properties props = new Properties();
        props.put("prefix.string",   "loaded");
        props.put("prefix.integer",  1);
        props.put("prefix.integer2", 2);
        props.put("prefix.boolean",  true);
        props.put("prefix.boolean2", true);
        props.put("prefix.short",    1);
        props.put("prefix.short2",   2);
        props.put("prefix.long",     1);
        props.put("prefix.long2",    2);
        props.put("prefix.float",    1.1);
        props.put("prefix.float2",   2.1);
        props.put("prefix.double",   1.1);
        props.put("prefix.double2",  2.1);
        
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withProperties(props)
                .withApplicationConfigName("application")
                .build();

        Assert.assertEquals("loaded", config.getString("prefix.string"));
        DefaultConfigMapper mapper = new DefaultConfigMapper();
        MyConfig c = mapper.newProxy(MyConfig.class, new PrefixedObservablePropertyFactory("prefix", config));

        Assert.assertEquals(1, c.getInteger());
        Assert.assertEquals(2, (int)c.getInteger2());
        Assert.assertEquals(true, c.getBoolean());
        Assert.assertEquals(true, c.getBoolean2());
        Assert.assertEquals(1, c.getShort());
        Assert.assertEquals(2, (short)c.getShort2());
        Assert.assertEquals(1, c.getLong());
        Assert.assertEquals(2, (long)c.getLong2());
        Assert.assertEquals(1.1f, c.getFloat(), 0);
        Assert.assertEquals(2.1f, (float)c.getFloat2(), 0);
        Assert.assertEquals(1.1, c.getDouble(), 0);
        Assert.assertEquals(2.1, (double)c.getDouble2(), 0);
        
    }
    
    @Test
    public void testProxyWithDefaults() {
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withApplicationConfigName("application")
                .build();

        DefaultConfigMapper mapper = new DefaultConfigMapper();
        MyConfig c = mapper.newProxy(MyConfig.class, new PrefixedObservablePropertyFactory("prefix", config));
        
        Assert.assertEquals(123, c.getInteger());
        Assert.assertNull(c.getInteger2());
        Assert.assertEquals(true, c.getBoolean());
        Assert.assertNull(c.getBoolean2());
        Assert.assertEquals(3, c.getShort());
        Assert.assertNull(c.getShort2());
        Assert.assertEquals(3, c.getLong());
        Assert.assertNull(c.getLong2());
        Assert.assertEquals(3.1f, c.getFloat(), 0);
        Assert.assertNull(c.getFloat2());
        Assert.assertEquals(3.1, c.getDouble(), 0);
        Assert.assertNull(c.getDouble2());
        
    }
}
