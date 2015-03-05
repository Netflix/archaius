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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CascadedPropertiesWithDeploymentContextTest {

    @Test
    public void testLoadCascadedPropertiesConfigDeployment() throws Exception {
        ConfigurationManager.getConfigInstance().setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_ENVIRONMENT_PROPERTY, "test");
        ConfigurationManager.getConfigInstance().setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_REGION_PROPERTY, "us-east-1");
        ConfigurationManager.loadCascadedPropertiesFromResources("test");
        assertEquals("9", ConfigurationManager.getConfigInstance().getProperty("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
        assertEquals("1", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
        ConfigurationManager.loadAppOverrideProperties("override");
        assertEquals("200", ConfigurationManager.getConfigInstance().getProperty("cascaded.property"));
    }
}
