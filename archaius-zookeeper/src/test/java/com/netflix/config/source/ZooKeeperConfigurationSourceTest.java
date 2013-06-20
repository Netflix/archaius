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
package com.netflix.config.source;

import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.test.TestingServer;
import com.netflix.curator.utils.DebugUtils;

/**
 * Tests the implementation of {@link ZooKeeperConfigurationSource}.
 * 
 * @author cfregly
 */
public class ZooKeeperConfigurationSourceTest {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfigurationSourceTest.class);

    private static final String CONFIG_ROOT_PATH = "/config";
    private static TestingServer server;
    private static CuratorFramework client;
    private static ZooKeeperConfigurationSource zkConfigSource; 
    private static ConcurrentMapConfiguration mapConfig; 
    
    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(DebugUtils.PROPERTY_DONT_LOG_CONNECTION_ISSUES, "true");
        server = new TestingServer();
        logger.info("Initialized local ZK with connect string [{}]", server.getConnectString());
        
        client = CuratorFrameworkFactory.newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
        client.start();

        zkConfigSource = new ZooKeeperConfigurationSource(client, CONFIG_ROOT_PATH);
        zkConfigSource.start();

        // setup system properties
        System.setProperty("test.key4", "test.value4-system");
        System.setProperty("test.key5", "test.value5-system");

        final ConcurrentMapConfiguration systemConfig = new ConcurrentMapConfiguration();
        systemConfig.loadProperties(System.getProperties());

        final DynamicWatchedConfiguration zkDynamicOverrideConfig = new DynamicWatchedConfiguration(zkConfigSource);

        mapConfig = new ConcurrentMapConfiguration();
        mapConfig.addProperty("test.key1", "test.value1-map");
        mapConfig.addProperty("test.key2", "test.value2-map");
        mapConfig.addProperty("test.key3", "test.value3-map");
        mapConfig.addProperty("test.key4", "test.value4-map");
        
        final ConcurrentCompositeConfiguration compositeConfig = new ConcurrentCompositeConfiguration();
        compositeConfig.addConfiguration(zkDynamicOverrideConfig, "zk dynamic override configuration");
        compositeConfig.addConfiguration(mapConfig, "map configuration");
        compositeConfig.addConfiguration(systemConfig, "system configuration");

        // setup ZK properties
        setZkProperty("test.key1", "test.value1-zk");
        setZkProperty("test.key2", "test.value2-zk");
        setZkProperty("test.key4", "test.value4-zk");

        ConfigurationManager.install(compositeConfig);
    }
    
    @Test
    public void testZkPropertyOverride() throws Exception {
        setZkProperty("test.key1", "test.value1-zk");
    	// there is an override from ZK, so make sure the overridden value is being returned
        Assert.assertEquals("test.value1-zk", DynamicPropertyFactory.getInstance()
                .getStringProperty("test.key1", "default").get());
    }
    
    @Test
    public void testNoZkPropertyOverride() throws Exception {
        // there's no override, so the map config value should be returned
        Assert.assertEquals("test.value3-map", DynamicPropertyFactory.getInstance().getStringProperty("test.key3", "default")
                .get());
    }
    
    @Test
    public void testDefault() throws Exception {
        // there's no property set, so the default should be returned
        Assert.assertEquals("default", DynamicPropertyFactory.getInstance().getStringProperty("test.key99", "default")
                .get());
    }
    
    @Test
    public void testSystemPropertyOverride() throws Exception {
        // there's a system property set, but this should not trump the zk override
        Assert.assertEquals("test.value4-zk", DynamicPropertyFactory.getInstance().getStringProperty("test.key4", "default")
                .get());
        
        // there's a system property set, but no other overrides, so should return the system property
        Assert.assertEquals("test.value5-system", DynamicPropertyFactory.getInstance().getStringProperty("test.key5", "default")
                .get());
    }
    
    @Test
    public void testUpdatePropertyOverride() throws Exception {
        setZkProperty("test.key1", "test.value1-zk");

        // update the map config's property and assert that the value is still the overridden value
        mapConfig.setProperty("test.key1", "prop1");
        Assert.assertEquals("test.value1-zk", DynamicPropertyFactory.getInstance()
                .getStringProperty("test.key1", "default").get());
    }
    
    @Test
    public void testUpdateZkProperty() throws Exception {                
        setZkProperty("test.key1", "test.value1-zk-override");
        
        Assert.assertEquals("test.value1-zk-override", DynamicPropertyFactory.getInstance()
                .getStringProperty("test.key1", "default").get());
    }

    @AfterClass
    public static void tearDown() throws Exception {
    	zkConfigSource.close();
    	server.close();
        logger.info("Tore down embedded ZK with connect string [{}]", server.getConnectString());
    }
    
    private static void setZkProperty(String key, String value) throws Exception {
        // update the underlying zk property and assert that the new value is picked up
        final CountDownLatch updateLatch = new CountDownLatch(1);
        zkConfigSource.addUpdateListener(new WatchedUpdateListener() {
            public void updateConfiguration(WatchedUpdateResult result) {
                updateLatch.countDown();
            }
        });
        zkConfigSource.setZkProperty(key, value);
        updateLatch.await();
    }
}
