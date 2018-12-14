package com.netflix.config.source;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.netflix.config.*;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Watcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the implementation of {@link K8sConfigurationSource}.
 *
 * @author luckyswede
 */
public class K8sConfigurationSourceTest {
    private static final Logger logger = LoggerFactory.getLogger(K8sConfigurationSourceTest.class);
    public static final ImmutableMap<String, String> K8S_CONFIG =
            ImmutableMap.of(
                    "test.key1", "test.value1-k8s",
                    "test.key4", "test.value4-k8s",
                    "test.key6", "test.value6-k8s",
                    "test.key7", "test.value7-k8s");

    private static K8sConfigurationSource K8S_CONFIGURATION_SOURCE;
    private static DynamicWatchedConfiguration K8S_CONFIGURATION;
    private static final ConcurrentMapConfiguration MAP_CONFIGURATION = new ConcurrentMapConfiguration();
    private static final ConcurrentMapConfiguration SYSTEM_CONFIGURATION = new ConcurrentMapConfiguration();

    @BeforeClass
    public static void before() throws Exception {
        final ConcurrentCompositeConfiguration compositeConfig = new ConcurrentCompositeConfiguration();
        K8S_CONFIGURATION_SOURCE = new K8sConfigurationSource("labelKey", "labelValue");
        K8S_CONFIGURATION = new DynamicWatchedConfiguration(K8S_CONFIGURATION_SOURCE);
        compositeConfig.addConfiguration(K8S_CONFIGURATION, "k8s dynamic override configuration");

        K8S_CONFIGURATION_SOURCE.configMapWatcher.eventReceived(Watcher.Action.ADDED, new ConfigMap(null, K8S_CONFIG, null, null));

        MAP_CONFIGURATION.addProperty("test.key1", "test.value1-map");
        MAP_CONFIGURATION.addProperty("test.key2", "test.value2-map");
        MAP_CONFIGURATION.addProperty("test.key3", "test.value3-map");
        MAP_CONFIGURATION.addProperty("test.key4", "test.value4-map");
        MAP_CONFIGURATION.addProperty("test.key7", "test.value7-map");
        compositeConfig.addConfiguration(MAP_CONFIGURATION, "map configuration");

        System.setProperty("test.key4", "test.value4-system");
        System.setProperty("test.key5", "test.value5-system");
        SYSTEM_CONFIGURATION.loadProperties(System.getProperties());
        compositeConfig.addConfiguration(SYSTEM_CONFIGURATION, "system configuration");

        ConfigurationManager.install(compositeConfig);
    }

    /**
     * should return value from K8sConfigurationSource when K8sConfigurationSource provides key
     */
    @Test
    public void testK8sPropertyOverride() throws Exception {
        assertEquals("test.value1-k8s", DynamicPropertyFactory.getInstance().getStringProperty("test.key1", "default").get());
    }

    /**
     * should return map configuration source value when K8sConfigurationSource does not provide key
     */
    @Test
    public void testNoK8sPropertyOverride() throws Exception {
        // there is not k8s value for this key but there is a configuration source that provides this key
        assertEquals("test.value2-map", DynamicPropertyFactory.getInstance().getStringProperty("test.key2", "default").get());
    }

    /**
     * should return default value when no configuration source provides key
     */
    @Test
    public void testDefault() throws Exception {
        // no configuration source for key
        assertEquals("default", DynamicPropertyFactory.getInstance().getStringProperty("test.key99", "default").get());
    }

    /**
     * should select lower priority configuration sources selected when K8sConfigurationSource does not provide key
     */
    @Test
    public void testSystemPropertyOverride() throws Exception {
        // system configuration provides key, k8s configuration provides key, source = k8s configuration
        assertEquals("test.value4-k8s", DynamicPropertyFactory.getInstance().getStringProperty("test.key4", "default").get());

        // system configuration provides key, k8s configuration does not provide key, source = system configuration
        assertEquals("test.value5-system", DynamicPropertyFactory.getInstance().getStringProperty("test.key5", "default").get());
    }

    /**
     * should not override K8sConfigurationSource when lower priority configuration source is updated
     */
    @Test
    public void testUpdateOverriddenProperty() throws Exception {
        final String updateProperty = "test.key1";

        // update the map config's property and assert that the value is still the overridden value
        MAP_CONFIGURATION.setProperty(updateProperty, "prop1");
        assertEquals("test.value1-k8s", DynamicPropertyFactory.getInstance().getStringProperty(updateProperty, "default").get());
    }

    /**
     * should return updated value from K8sConfigurationSource
     */
    @Test
    public void testUpdatedK8sProperty() throws Exception {
        Map<String, String> updatedMap = Maps.newHashMap(K8S_CONFIG);
        updatedMap.put("test.key6", "test.value6-k8s-override");
        K8S_CONFIGURATION_SOURCE.configMapWatcher.eventReceived(Watcher.Action.ADDED, new ConfigMap(null, updatedMap, null, null));
        assertEquals("test.value6-k8s-override", DynamicPropertyFactory.getInstance().getStringProperty("test.key6", "default").get());
    }

    /**
     * should not return value from K8sConfigurationSource
     */
    @Test
    public void testDeleteK8sProperty() throws Exception {
        Map<String, String> updatedMap = Maps.newHashMap(K8S_CONFIG);
        updatedMap.remove("test.key7");
        K8S_CONFIGURATION_SOURCE.configMapWatcher.eventReceived(Watcher.Action.ADDED, new ConfigMap(null, updatedMap, null, null));
        assertEquals("test.value7-map", DynamicPropertyFactory.getInstance().getStringProperty("test.key7", "default").get());
    }
}
