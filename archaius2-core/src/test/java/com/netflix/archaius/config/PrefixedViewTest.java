package com.netflix.archaius.config;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.instrumentation.AccessMonitorUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PrefixedViewTest {
    @Test
    public void confirmNotificactionOnAnyChange() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build();
        
        Config prefix = config.getPrefixedView("foo");
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        
        prefix.addListener(listener);
        
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(any());
        
        config.addConfig("bar", DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(any());
    }
    
    @Test
    public void confirmNotifactionOnSettableConfigChange() throws ConfigException {
        SettableConfig settable = new DefaultSettableConfig();
        settable.setProperty("foo.bar", "original");
        
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("settable", settable)
                .build();
        
        Config prefix = config.getPrefixedView("foo");
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        prefix.addListener(listener);
        
        // Confirm original state
        assertEquals("original", prefix.getString("bar"));
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(any());

        // Update the property and confirm onConfigUpdated notification
        settable.setProperty("foo.bar", "new");
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
        assertEquals("new", prefix.getString("bar"));
        
        // Add a new config and confirm onConfigAdded notification
        config.addConfig("new", MapConfig.builder().put("foo.bar", "new2").build());
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(any());
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
    }

    @Test
    public void trailingDotAllowed() {
        SettableConfig settable = new DefaultSettableConfig();
        settable.setProperty("foo.bar", "value");

        Config prefixNoDot = settable.getPrefixedView("foo");
        Config prefixWithDot = settable.getPrefixedView("foo.");

        assertEquals(prefixNoDot.getString("bar"), "value");
        assertEquals(prefixWithDot.getString("bar"), "value");
    }

    @Test
    public void unusedPrefixedViewIsGarbageCollected() {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        Config prefix = sourceConfig.getPrefixedView("foo.");
        Reference<Config> weakReference = new WeakReference<>(prefix);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        prefix = null;
        System.gc();
        assertNull(weakReference.get());
    }

    @Test
    public void testGetKeys() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        Set<String> keySet = new HashSet<>();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("prop1"));
        assertTrue(keySet.contains("prop2"));
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        keys.next();
        assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        Iterable<String> keys = config.keys();
        assertEquals(2, Iterables.size(keys));
        assertEquals(Sets.newHashSet("prop1", "prop2"), Sets.newHashSet(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }

    @Test
    public void instrumentationNotEnabled() throws Exception {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        assertFalse(config.instrumentationEnabled());
        assertEquals(config.getRawProperty("prop1"), "value1");
        assertEquals(config.getRawProperty("prop2"), "value2");
    }

    @Test
    public void instrumentation() throws Exception {
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

        Config config = baseConfig.getPrefixedView("foo");

        assertTrue(config.instrumentationEnabled());

        config.getRawProperty("prop1");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("foo.prop1", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.getRawPropertyUninstrumented("prop2");
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        config.forEachProperty((k, v) -> {});
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("foo.prop1", "1", "foo-value")));
        verify(accessMonitorUtil, times(1)).registerUsage(eq(new PropertyDetails("foo.prop2", "2", "bar-value")));
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        config.forEachPropertyUninstrumented((k, v) -> {});
        verify(accessMonitorUtil, times(3)).registerUsage(any());
    }
}
