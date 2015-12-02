package com.netflix.config;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: michaels@netflix.com
 * Date: 11/30/15
 * Time: 2:35 PM
 */
public class DynamicPropertyCachedPrimitiveTest
{
    @Before
    public void setup() {
        ConfigurationManager.getConfigInstance().clear();
    }

    @Test
    public void testUpdatingValueAlsoUpdatesCachedPrimitive_Boolean() {
        // Create property with initial value.
        String propName = "testprop1";
        ConfigurationManager.getConfigInstance().setProperty(propName, "true");
        DynamicBooleanProperty prop = DynamicPropertyFactory.getInstance().getBooleanProperty(propName, false);

        assertTrue(prop.get());
        assertTrue(prop.getValue().booleanValue());

        // Change the value of the property.
        ConfigurationManager.getConfigInstance().setProperty(propName, "false");
        assertFalse(prop.get());
        assertFalse(prop.getValue().booleanValue());

        // And change back again.
        ConfigurationManager.getConfigInstance().setProperty(propName, "true");
        assertTrue(prop.get());
        assertTrue(prop.getValue().booleanValue());

        // Remove the prop value, should change to default value.
        ConfigurationManager.getConfigInstance().clearProperty(propName);
        assertFalse(prop.get());
        assertFalse(prop.getValue().booleanValue());
    }

    @Test
    public void testUpdatingValueAlsoUpdatesCachedPrimitive_Int() {
        // Create property with initial value.
        String propName = "testprop2";
        ConfigurationManager.getConfigInstance().setProperty(propName, "1");
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty(propName, 0);

        assertEquals(1, prop.get());
        assertEquals(1, prop.getValue().intValue());

        // Change the value of the property.
        ConfigurationManager.getConfigInstance().setProperty(propName, "2");
        assertEquals(2, prop.get());
        assertEquals(2, prop.getValue().intValue());

        // Remove the prop value, should change to default value.
        ConfigurationManager.getConfigInstance().clearProperty(propName);
        assertEquals(0, prop.get());
        assertEquals(0, prop.getValue().intValue());
    }

    @Test
    public void testUpdatingValueAlsoUpdatesCachedPrimitive_Long() {
        // Create property with initial value.
        String propName = "testprop3";
        ConfigurationManager.getConfigInstance().setProperty(propName, "1");
        DynamicLongProperty prop = DynamicPropertyFactory.getInstance().getLongProperty(propName, 0);

        assertEquals(1, prop.get());
        assertEquals(1, prop.getValue().longValue());

        // Change the value of the property.
        ConfigurationManager.getConfigInstance().setProperty(propName, "2");
        assertEquals(2, prop.get());
        assertEquals(2, prop.getValue().longValue());

        // Remove the prop value, should change to default value.
        ConfigurationManager.getConfigInstance().clearProperty(propName);
        assertEquals(0, prop.get());
        assertEquals(0, prop.getValue().longValue());
    }

    @Test
    public void testUpdatingValueAlsoUpdatesCachedPrimitive_Double() {
        // Create property with initial value.
        String propName = "testprop3";
        ConfigurationManager.getConfigInstance().setProperty(propName, "1.1");
        DynamicDoubleProperty prop = DynamicPropertyFactory.getInstance().getDoubleProperty(propName, 0.5d);

        assertEquals(1.1d, prop.get());
        assertEquals(1.1d, prop.getValue().doubleValue());

        // Change the value of the property.
        ConfigurationManager.getConfigInstance().setProperty(propName, "2.2");
        assertEquals(2.2d, prop.get());
        assertEquals(2.2d, prop.getValue().doubleValue());

        // Remove the prop value, should change to default value.
        ConfigurationManager.getConfigInstance().clearProperty(propName);
        assertEquals(0.5d, prop.get());
        assertEquals(0.5d, prop.getValue().doubleValue());
    }

    @Test
    public void testUpdatingValueAlsoUpdatesCachedPrimitive_Float() {
        // Create property with initial value.
        String propName = "testprop4";
        ConfigurationManager.getConfigInstance().setProperty(propName, "1.1");
        DynamicFloatProperty prop = DynamicPropertyFactory.getInstance().getFloatProperty(propName, 0.5f);

        assertEquals(1.1f, prop.get());
        assertEquals(1.1f, prop.getValue().floatValue());

        // Change the value of the property.
        ConfigurationManager.getConfigInstance().setProperty(propName, "2.2");
        assertEquals(2.2f, prop.get());
        assertEquals(2.2f, prop.getValue().floatValue());

        // Remove the prop value, should change to default value.
        ConfigurationManager.getConfigInstance().clearProperty(propName);
        assertEquals(0.5f, prop.get());
        assertEquals(0.5f, prop.getValue().floatValue());
    }
}
