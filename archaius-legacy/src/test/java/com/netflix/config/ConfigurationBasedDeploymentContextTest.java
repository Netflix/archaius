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

import org.apache.commons.configuration.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationBasedDeploymentContextTest {
    private static Configuration config = ConfigurationManager.getConfigInstance();

    @BeforeClass
    public static void beforeClass() throws Exception {
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_APPLICATION_ID_PROPERTY, "appId1");
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_DATACENTER_PROPERTY, "datacenter1");
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_ENVIRONMENT_PROPERTY, "environment1");
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_REGION_PROPERTY, "region1");
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_SERVER_ID_PROPERTY, "serverId1");
        config.setProperty(ConfigurationBasedDeploymentContext.DEPLOYMENT_STACK_PROPERTY, "stack1");
    }

    @Test
    public void testGetAndSetAppId() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getApplicationId(), "appId1");
        assertEquals(config.getString(DeploymentContext.ContextKey.appId.getKey()), "appId1");
        context.setApplicationId("appId2");
        assertEquals(context.getApplicationId(), "appId2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.appId,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_APPLICATION_ID_PROPERTY,
                "appId2"));
    }

    @Test
    public void testGetAndSetDatacenter() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getDeploymentDatacenter(), "datacenter1");
        assertEquals(config.getString(DeploymentContext.ContextKey.datacenter.getKey()), "datacenter1");
        context.setDeploymentDatacenter("datacenter2");
        assertEquals(context.getDeploymentDatacenter(), "datacenter2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.datacenter,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_DATACENTER_PROPERTY,
                "datacenter2"));
    }

    @Test
    public void testGetAndSetEnvironment() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getDeploymentEnvironment(), "environment1");
        assertEquals(config.getString(DeploymentContext.ContextKey.environment.getKey()), "environment1");
        context.setDeploymentEnvironment("environment2");
        assertEquals(context.getDeploymentEnvironment(), "environment2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.environment,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_ENVIRONMENT_PROPERTY,
                "environment2"));
    }

    @Test
    public void testGetAndSetRegion() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getDeploymentRegion(), "region1");
        assertEquals(config.getString(DeploymentContext.ContextKey.region.getKey()), "region1");
        context.setDeploymentRegion("region2");
        assertEquals(context.getDeploymentRegion(), "region2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.region,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_REGION_PROPERTY,
                "region2"));
    }

    @Test
    public void testGetAndSetServerId() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getDeploymentServerId(), "serverId1");
        assertEquals(config.getString(DeploymentContext.ContextKey.serverId.getKey()), "serverId1");
        context.setDeploymentServerId("server2");
        assertEquals(context.getDeploymentServerId(), "server2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.serverId,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_SERVER_ID_PROPERTY,
                "server2"));
    }

    @Test
    public void testGetAndSetStack() throws Exception {
        DeploymentContext context = ConfigurationManager.getDeploymentContext();
        assertEquals(context.getDeploymentStack(), "stack1");
        assertEquals(config.getString(DeploymentContext.ContextKey.stack.getKey()), "stack1");
        context.setDeploymentStack("stack2");
        assertEquals(context.getDeploymentStack(), "stack2");
        assertTrue(testPropertyValues(DeploymentContext.ContextKey.stack,
                ConfigurationBasedDeploymentContext.DEPLOYMENT_STACK_PROPERTY,
                "stack2"));
    }

    private boolean testPropertyValues(DeploymentContext.ContextKey key, String deploymentPropName, String expectedValue) {
        return config.getString(key.getKey()).equals(expectedValue) &&
                config.getString(deploymentPropName).equals(expectedValue);
    }
}
