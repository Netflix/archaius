/**
 * Copyright 2013 Netflix, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationManagerTest {

    static DynamicStringProperty prop1 = DynamicPropertyFactory.getInstance().getStringProperty("prop1", null);
    
    @Test
    public void testInstall() {
        ConfigurationManager.getConfigInstance().setProperty("prop1", "abc");
        assertEquals("abc", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("abc", prop1.get());
        BaseConfiguration newConfig = new BaseConfiguration();
        newConfig.setProperty("prop1", "fromNewConfig");
        ConfigurationManager.install(newConfig);
        assertEquals("fromNewConfig", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("fromNewConfig", prop1.get());
        newConfig.setProperty("prop1", "changed");
        assertEquals("changed", ConfigurationManager.getConfigInstance().getProperty("prop1"));
        assertEquals("changed", prop1.get());
        try {
            ConfigurationManager.install(new BaseConfiguration());
            fail("IllegalStateExceptionExpected");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        try {
            DynamicPropertyFactory.initWithConfigurationSource(new BaseConfiguration());
            fail("IllegalStateExceptionExpected");
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
    }
    
    @Test
    public void testLoadProperties() throws Exception {
        ConfigurationManager.loadPropertiesFromResources("test.properties");
        assertEquals("5", ConfigurationManager.getConfigInstance().getProperty("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
    }
    
    @Test
    public void testLoadCascadedProperties() throws Exception {
        SimpleDeploymentContext context = new SimpleDeploymentContext();
        context.setDeploymentEnvironment("test");
        context.setDeploymentRegion("us-east-1");
        ConfigurationManager.setDeploymentContext(context);
        ConfigurationManager.loadCascadedPropertiesFromResources("test");
        assertEquals("9", ConfigurationManager.getConfigInstance().getProperty("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
        assertEquals("1", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
        ConfigurationManager.loadAppOverrideProperties("override");
        assertEquals("200", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
    }
}
