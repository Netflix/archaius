package com.netflix.config.source;

import com.google.common.collect.Lists;
import com.netflix.config.*;
import org.boon.core.Handler;
import org.boon.etcd.ClientBuilder;
import org.boon.etcd.Etcd;
import org.boon.etcd.Node;
import org.boon.etcd.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests the implementation of {@link EtcdConfigurationSource}.
 *
 * @author spoon16
 */
public class EtcdConfigurationSourceTest {
    private static final Logger logger = LoggerFactory.getLogger(EtcdConfigurationSourceTest.class);

    private static final Etcd ETCD = mock(Etcd.class);

    // uncomment to use local/vagrant CoreOS VM running Etcd
    // private static final Etcd ETCD = ClientBuilder.builder().hosts(URI.create("http://172.17.8.101:4001")).createClient();

    private static final String CONFIG_PATH = "config";
    private static final Response ETCD_LIST_RESPONSE = new Response("get", 200,
            new Node("/config", null, 1378, 1378, 0, true, Lists.newArrayList(
                    new Node("/config/test.key1", "test.value1-etcd", 19311, 19311, 0, false, null),
                    new Node("/config/test.key4", "test.value4-etcd", 1388, 1388, 0, false, null),
                    new Node("/config/test.key6", "test.value6-etcd", 1232, 1232, 0, false, null),
                    new Node("/config/test.key7", "test.value7-etcd", 1234, 1234, 0, false, null)
            )));
    private static Handler<Response> ETCD_UPDATE_HANDLER;
    private static final Answer WITH_ETCD_UPDATE_HANDLER = new Answer() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            ETCD_UPDATE_HANDLER = (Handler<Response>) invocation.getArguments()[0];
            return null;
        }
    };
    private static EtcdConfigurationSource ETCD_CONFIGURATION_SOURCE;
    private static DynamicWatchedConfiguration ETCD_CONFIGURATION;
    private static final ConcurrentMapConfiguration MAP_CONFIGURATION = new ConcurrentMapConfiguration();
    private static final ConcurrentMapConfiguration SYSTEM_CONFIGURATION = new ConcurrentMapConfiguration();

    @BeforeClass
    public static void before() throws Exception {
        final ConcurrentCompositeConfiguration compositeConfig = new ConcurrentCompositeConfiguration();

        doReturn(ETCD_LIST_RESPONSE).when(ETCD).list(anyString());
        doAnswer(WITH_ETCD_UPDATE_HANDLER).when(ETCD).waitRecursive(any(Handler.class), anyString());
        ETCD_CONFIGURATION_SOURCE = new EtcdConfigurationSource(ETCD, CONFIG_PATH);
        ETCD_CONFIGURATION = new DynamicWatchedConfiguration(ETCD_CONFIGURATION_SOURCE);

        compositeConfig.addConfiguration(ETCD_CONFIGURATION, "etcd dynamic override configuration");

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
     * should return value from EtcdConfigurationSource when EtcdConfigurationSource provides key
     */
    @Test
    public void testEtcdPropertyOverride() throws Exception {
        // there is a etcd value for this key
        assertEquals("test.value1-etcd", DynamicPropertyFactory.getInstance().getStringProperty("test.key1", "default").get());
    }

    /**
     * should return map configuration source value when EtcdConfigurationSource does not provide key
     */
    @Test
    public void testNoEtcdPropertyOverride() throws Exception {
        // there is not etcd value for this key but there is a configuration source that provides this key
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
     * should select lower priority configuration sources selected when EtcdConfigurationSource does not provide key
     */
    @Test
    public void testSystemPropertyOverride() throws Exception {
        // system configuration provides key, etcd configuration provides key, source = etcd configuration
        assertEquals("test.value4-etcd", DynamicPropertyFactory.getInstance().getStringProperty("test.key4", "default").get());

        // system configuration provides key, etcd configuration does not provide key, source = system configuration
        assertEquals("test.value5-system", DynamicPropertyFactory.getInstance().getStringProperty("test.key5", "default").get());
    }

    /**
     * should not override EtcdConfigurationSource when lower priority configuration source is updated
     */
    @Test
    public void testUpdateOverriddenProperty() throws Exception {
        final String updateProperty = "test.key1";

        // update the map config's property and assert that the value is still the overridden value
        MAP_CONFIGURATION.setProperty(updateProperty, "prop1");
        assertEquals("test.value1-etcd", DynamicPropertyFactory.getInstance().getStringProperty(updateProperty, "default").get());
    }

    /**
     * should update EtcdConfigurationSource when Etcd client handles writes
     */
    @Test
    public void testUpdateEtcdProperty() throws Exception {
        final String updateProperty = "test.key6";
        final String updateKey = CONFIG_PATH + "/" + updateProperty;
        final String updateValue = "test.value6-etcd-override";
        final String initialValue = "test.value6-etcd";

        assertEquals(initialValue, DynamicPropertyFactory.getInstance().getStringProperty(updateProperty, "default").get());

        ETCD_UPDATE_HANDLER.handle(new Response("set", 200, new Node(updateKey, updateValue, 19444, 19444, 0, false, null)));
        assertEquals(updateValue, DynamicPropertyFactory.getInstance().getStringProperty(updateProperty, "default").get());
    }

    /**
     * should delete from EtcdConfigurationSource when Etcd client handles a delete event
     */
    @Test
    public void testDeleteEtcdProperty() throws Exception {
        final String deleteProperty = "test.key7";
        final String deleteKey = CONFIG_PATH + "/" + deleteProperty;
        final String initialValue = "test.value7-etcd";

        assertEquals(initialValue, DynamicPropertyFactory.getInstance().getStringProperty(deleteProperty, "default").get());

        ETCD_UPDATE_HANDLER.handle(new Response("delete", 200, new Node(deleteKey, null, 12345, 12345, 0, false, null)));
        assertEquals("test.value7-map", DynamicPropertyFactory.getInstance().getStringProperty(deleteProperty, "default").get());
    }
}
