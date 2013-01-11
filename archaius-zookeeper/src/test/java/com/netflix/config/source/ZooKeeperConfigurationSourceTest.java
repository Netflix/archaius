package com.netflix.config.source;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.test.TestingServer;
import com.netflix.curator.utils.DebugUtils;

public class ZooKeeperConfigurationSourceTest {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfigurationSourceTest.class);

    private static ZooKeeperConfigurationSource configSource;
    private static final String CONFIG_ROOT_PATH = "/config";
    private static TestingServer server;
    private static PathChildrenCache pathChildrenCache;
    private static CuratorFramework client;
    
    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(DebugUtils.PROPERTY_DONT_LOG_CONNECTION_ISSUES, "true");
        server = new TestingServer();
        logger.trace("Initialized local ZK with connect string [{}]", server.getConnectString());
        
        client = CuratorFrameworkFactory.newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
        client.start();
        
        configSource = new ZooKeeperConfigurationSource(client, CONFIG_ROOT_PATH);
        
        pathChildrenCache = new PathChildrenCache(client, CONFIG_ROOT_PATH, true);
        pathChildrenCache.start();
    }
    
    @Test
    public void testCascade() throws Exception {
        // setup system properties
        System.setProperty("test.key4", "test.value4-system");
        
        // setup ZK properties
        configSource.setZkProperty("test.key1", "test.value1-zk");
        configSource.setZkProperty("test.key2", "test.value2-zk");
        configSource.setZkProperty("test.key4", "test.value4-zk");

        final ConcurrentMapConfiguration systemConfig = new ConcurrentMapConfiguration();
        systemConfig.loadProperties(System.getProperties());

        final DynamicWatchedConfiguration zkDynamicOverrideConfig = new DynamicWatchedConfiguration(configSource);

        final ConcurrentMapConfiguration mapConfig = new ConcurrentMapConfiguration();
        mapConfig.addProperty("test.key1", "test.value1-map");
        mapConfig.addProperty("test.key2", "test.value2-map");
        mapConfig.addProperty("test.key3", "test.value3-map");
        mapConfig.addProperty("test.key4", "test.value4-map");
        
        final ConcurrentCompositeConfiguration compositeConfig = new ConcurrentCompositeConfiguration();
        compositeConfig.addConfiguration(systemConfig, "system configuration");
        compositeConfig.addConfiguration(zkDynamicOverrideConfig, "zk dynamic override configuration");
        compositeConfig.addConfiguration(mapConfig, "map configuration");

        ConfigurationManager.install(compositeConfig);

        // there is an override from ZK, so make sure the overridden value is being returned
        Assert.assertEquals("test.value1-zk", DynamicPropertyFactory.getInstance()
                .getStringProperty("test.key1", "default").get());

        // there's no override, so the map config value should be returned
        Assert.assertEquals("test.value3-map", DynamicPropertyFactory.getInstance().getStringProperty("test.key3", "default")
                .get());

        // there's no property set, so the default should be returned
        Assert.assertEquals("default", DynamicPropertyFactory.getInstance().getStringProperty("test.key99", "default")
                .get());
        
        // there's a system property set, so this should trump the zk and map config overrides
        Assert.assertEquals("test.value4-system", DynamicPropertyFactory.getInstance().getStringProperty("test.key4", "default")
                .get());

        // update the main config's property
        // assert that the value is still the overridden value despite changing the map config
        mapConfig.setProperty("test.key1", "prop1");
        Assert.assertEquals("test.value1-zk", DynamicPropertyFactory.getInstance()
                .getStringProperty("test.key1", "default").get());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.close();
        logger.trace("Tore down embedded ZK with connect string [{}]", server.getConnectString());
    }
}