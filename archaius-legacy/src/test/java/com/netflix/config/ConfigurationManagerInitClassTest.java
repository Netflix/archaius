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
package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationManagerInitClassTest {
    
    @BeforeClass
    public static void init() {
        System.setProperty("archaius.default.configuration.class", "com.netflix.config.TestConfiguration");        
    }

    @Test
    public void testConfigurationClass() {
        TestConfiguration config = (TestConfiguration) ConfigurationManager.getConfigInstance();
        assertTrue(ConfigurationManager.isConfigurationInstalled());
        Object configSource = DynamicPropertyFactory.getInstance().getBackingConfigurationSource();
        assertTrue(configSource == config);
        try {
            ConfigurationManager.install(new BaseConfiguration());
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }        
        
        
    }
}

class TestConfiguration extends BaseConfiguration {
    
}
