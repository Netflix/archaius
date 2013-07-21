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
        assertEquals(config.getString(DeploymentContext.ContextKey.appId.getKey()), null);
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
        assertEquals(config.getString(DeploymentContext.ContextKey.datacenter.getKey()), null);
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
        assertEquals(config.getString(DeploymentContext.ContextKey.environment.getKey()), null);
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
        assertEquals(config.getString(DeploymentContext.ContextKey.region.getKey()), null);
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
        assertEquals(config.getString(DeploymentContext.ContextKey.serverId.getKey()), null);
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
        assertEquals(config.getString(DeploymentContext.ContextKey.stack.getKey()), null);
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
