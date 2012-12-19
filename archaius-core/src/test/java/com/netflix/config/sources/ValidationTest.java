package com.netflix.config.sources;

import static org.junit.Assert.*;

import org.junit.Test;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.validation.ValidationException;

public class ValidationTest {
    
    @Test
    public void testValidation() {
        DynamicStringProperty prop = new DynamicStringProperty("abc", "default") {
            public void validate(String newValue) {
                throw new ValidationException("failed");
            }
        };
        try {
            ConfigurationManager.getConfigInstance().setProperty("abc", "new");
            fail("ValidationException expected");
        } catch (ValidationException e) {
            assertNotNull(e);
        }
        assertEquals("default", prop.get());
        assertNull(ConfigurationManager.getConfigInstance().getProperty("abc"));
        
        try {
            ConfigurationManager.getConfigInstance().addProperty("abc", "new");
            fail("ValidationException expected");
        } catch (ValidationException e) {
            assertNotNull(e);
        }
        assertEquals("default", prop.get());
        assertNull(ConfigurationManager.getConfigInstance().getProperty("abc"));
    }
}
