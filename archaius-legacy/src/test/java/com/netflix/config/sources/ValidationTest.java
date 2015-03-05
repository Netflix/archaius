/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
