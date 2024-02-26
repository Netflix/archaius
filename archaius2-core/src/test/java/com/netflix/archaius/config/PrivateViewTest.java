package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.instrumentation.AccessMonitorUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.netflix.archaius.TestUtils.set;
import static com.netflix.archaius.TestUtils.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PrivateViewTest {

    @Test
    public void decoderSettingDoesNotPropagate() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build();

        Config privateView = config.getPrivateView();

        Decoder privateDecoder = Mockito.mock(Decoder.class);
        privateView.setDecoder(privateDecoder);

        assertNotSame(config.getDecoder(), privateView.getDecoder());

        Decoder newUpstreamDecoder = Mockito.mock(Decoder.class);
        config.setDecoder(newUpstreamDecoder);

        assertNotSame(config.getDecoder(), privateView.getDecoder());

        assertSame(privateDecoder, privateView.getDecoder());
    }

    @Test
    public void confirmNotificationOnAnyChange() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build();
        
        Config privateView = config.getPrivateView();
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        
        privateView.addListener(listener);
        
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(any());
        
        config.addConfig("bar", DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(any());
    }
    
    @Test
    public void confirmNotificationOnSettableConfigChange() throws ConfigException {
        SettableConfig settable = new DefaultSettableConfig();
        settable.setProperty("foo.bar", "original");
        
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("settable", settable)
                .build();
        
        Config privateView = config.getPrivateView();
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        privateView.addListener(listener);
        
        // Confirm original state
        assertEquals("original", privateView.getString("foo.bar"));
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(any());

        // Update the property and confirm onConfigUpdated notification
        settable.setProperty("foo.bar", "new");
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
        assertEquals("new", privateView.getString("foo.bar"));
        
        // Add a new config and confirm onConfigAdded notification
        config.addConfig("new", MapConfig.builder().put("foo.bar", "new2").build());
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(any());
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
    }
    @Test
    public void unusedPrivateViewIsGarbageCollected() {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        Config privateView = sourceConfig.getPrivateView();
        Reference<Config> weakReference = new WeakReference<>(privateView);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        privateView = null;
        System.gc();
        assertNull(weakReference.get());
    }

    @Test
    public void testGetKeys() {
        Config config = MapConfig.builder()
                .put("foo", "foo-value")
                .put("bar", "bar-value")
                .build()
                .getPrivateView();

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        Set<String> keySet = new HashSet<>();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("foo"));
        assertTrue(keySet.contains("bar"));
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() {
        Config config = MapConfig.builder()
                .put("foo", "foo-value")
                .put("bar", "bar-value")
                .build()
                .getPrivateView();

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        keys.next();
        assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() {
        Config config = MapConfig.builder()
                .put("foo", "foo-value")
                .put("bar", "bar-value")
                .build()
                .getPrivateView();

        Iterable<String> keys = config.keys();
        assertEquals(2, size(keys));
        assertEquals(set("foo", "bar"), set(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        Config config = MapConfig.builder()
                .put("foo", "foo-value")
                .put("bar", "bar-value")
                .build()
                .getPrivateView();

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }

    @Test
    public void instrumentationNotEnabled() throws Exception {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrivateView();

        assertFalse(config.instrumentationEnabled());
        assertEquals(config.getRawProperty("foo.prop1"), "value1");
        assertEquals(config.getRawProperty("foo.prop2"), "value2");
    }

    @Test
    public void testInstrumentation() throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Callable<PollingResponse> reader = () -> {
            Map<String, String> props = new HashMap<>();
            props.put("foo.prop1", "foo-value");
            props.put("foo.prop2", "bar-value");
            Map<String, String> propIds = new HashMap<>();
            propIds.put("foo.prop1", "1");
            propIds.put("foo.prop2", "2");
            return PollingResponse.forSnapshot(props, propIds);
        };
        AccessMonitorUtil accessMonitorUtil = spy(AccessMonitorUtil.builder().build());
        PollingDynamicConfig baseConfig = new PollingDynamicConfig(reader, strategy, accessMonitorUtil);
        strategy.fire();

        Config config = baseConfig.getPrivateView();

        assertTrue(config.instrumentationEnabled());

        config.getRawProperty("foo.prop1");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("foo.prop1", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.getRawPropertyUninstrumented("foo.prop2");
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.forEachProperty((k, v) -> {});
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("foo.prop1", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(eq(new PropertyDetails("foo.prop2", "2", "bar-value")));
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        config.forEachPropertyUninstrumented((k, v) -> {});
        verify(accessMonitorUtil, times(3)).registerUsage(any());
    }
}
