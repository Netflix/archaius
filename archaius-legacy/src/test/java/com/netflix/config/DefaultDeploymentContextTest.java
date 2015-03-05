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

import org.apache.commons.configuration.CompositeConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultDeploymentContextTest {
    
    @BeforeClass
    public static void init() {
        System.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_REGION_PROPERTY, "us-east-1");
    }
    
    @Test
    public void testGetRegion() {
        String region = ConfigurationManager.getConfigInstance().getString("@region");
        assertEquals("us-east-1", region);
        
        ConfigurationManager.getConfigInstance().setProperty(DeploymentContext.ContextKey.region.getKey(), "us-west-2");
        assertEquals("us-west-2", ConfigurationManager.getDeploymentContext().getDeploymentRegion());
        
        ((ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance()).setOverrideProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_REGION_PROPERTY, "us-east-1");
        assertEquals("us-east-1", ConfigurationManager.getDeploymentContext().getDeploymentRegion());
        assertEquals("us-east-1", ConfigurationManager.getConfigInstance().getProperty(DeploymentContext.ContextKey.region.getKey()));
    }
}
