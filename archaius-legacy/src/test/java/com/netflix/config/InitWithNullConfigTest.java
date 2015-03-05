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

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

public class InitWithNullConfigTest {
    
    @BeforeClass
    public static void init() {
        System.setProperty(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG, "true");
    }
    
    @Test
    public void testCreateProperty() {
        try {
            DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration) null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        DynamicBooleanProperty prop = DynamicPropertyFactory.getInstance().getBooleanProperty("abc", false);
        BaseConfiguration baseConfig = new BaseConfiguration();
        DynamicPropertyFactory.initWithConfigurationSource(baseConfig);
        baseConfig.setProperty("abc", "true");
        assertTrue(prop.get());
    }
}
