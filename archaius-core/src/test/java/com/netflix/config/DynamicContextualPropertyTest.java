package com.netflix.config;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.config.DynamicContextualProperty.Value;

public class DynamicContextualPropertyTest {
    

    @Test
    public void testPropertyChange() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        List<Value<Integer>> values = Lists.newArrayList();
        Value<Integer> value = new Value<Integer>();
        Map<String, Collection<String>> dimension = Maps.newHashMap();
        dimension.put("d1", Lists.newArrayList("v1", "v2"));
        value.setDimensions(dimension);
        value.setValue(5);
        values.add(value);
        
        Value<Integer> value2 = new Value<Integer>();
        Map<String, Collection<String>> dimension2 = Maps.newHashMap();
        dimension2.put("d1", Lists.newArrayList("v3"));
        dimension2.put("d2", Lists.newArrayList("x1"));
        value2.setDimensions(dimension2);
        value2.setValue(10);
        values.add(value2);
        
        value = new Value<Integer>();
        value.setValue(2);
        values.add(value);
        
        String json = mapper.writeValueAsString(values);
        System.out.println("serialized json: " + json);        
        ConfigurationManager.getConfigInstance().setProperty("d1", "v1");
        ConfigurationManager.getConfigInstance().setProperty("contextualProp", json);
        DynamicContextualProperty<Integer> prop = new DynamicContextualProperty<Integer>("contextualProp", 0);
        // d1=v1
        assertEquals(5, prop.getValue().intValue());

        // d1=v2
        ConfigurationManager.getConfigInstance().setProperty("d1", "v2");
        assertEquals(5, prop.getValue().intValue());

        // d1=v3
        ConfigurationManager.getConfigInstance().setProperty("d1", "v3");
        assertEquals(2, prop.getValue().intValue());

        // d1=v3, d2 = x1
        ConfigurationManager.getConfigInstance().setProperty("d2", "x1");
        assertEquals(10, prop.getValue().intValue());

        // d1=v1, d2 = x1
        ConfigurationManager.getConfigInstance().setProperty("d1", "v1");
        assertEquals(5, prop.getValue().intValue());

        values.remove(0);        
        json = mapper.writeValueAsString(values);
        ConfigurationManager.getConfigInstance().setProperty("contextualProp", json);
        assertEquals(2, prop.getValue().intValue());
        
        ConfigurationManager.getConfigInstance().clearProperty("contextualProp");
        
        assertEquals(0, prop.getValue().intValue());
    }
    
    @Test
    public void testInvalidJson() {
        String invalidJson = "invalidJson";        
        ConfigurationManager.getConfigInstance().setProperty("testInvalid", invalidJson);
        DynamicContextualProperty<Integer> prop = new DynamicContextualProperty<Integer>("key1", 0);
        try {
            new DynamicContextualProperty<Integer>("testInvalid", 0);
            fail("Exception expected");
        } catch (Exception e) {
            assertNotNull(e);
        }
        ConfigurationManager.getConfigInstance().setProperty("key1", invalidJson);
        // should not throw exception and just return default
        assertEquals(0, prop.getValue().intValue());
        
    }
}
