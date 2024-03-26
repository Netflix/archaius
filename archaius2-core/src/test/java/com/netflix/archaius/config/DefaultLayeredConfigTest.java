package com.netflix.archaius.config;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.netflix.archaius.Layers;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.api.config.LayeredConfig;
import com.netflix.archaius.api.config.SettableConfig;

import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.instrumentation.AccessMonitorUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultLayeredConfigTest {
    @Test
    public void validateApiOnEmptyConfig() {
        LayeredConfig config = new DefaultLayeredConfig();
        
        assertFalse(config.getProperty("propname").isPresent());
        assertNull(config.getRawProperty("propname"));

        @SuppressWarnings("unchecked")
        LayeredConfig.LayeredVisitor<String> visitor = Mockito.mock(LayeredConfig.LayeredVisitor.class);
        config.accept(visitor);
        Mockito.verify(visitor, Mockito.never()).visitConfig(any(), any());
        Mockito.verify(visitor, Mockito.never()).visitKey(any(), any());
    }
    
    @Test
    public void validateListenerCalled() {
        // Setup main config
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        LayeredConfig config = new DefaultLayeredConfig();
        config.addListener(listener);
        
        // Add a child
        config.addConfig(Layers.APPLICATION, new DefaultSettableConfig());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
    }
    
    @Test
    public void validatePropertyUpdates() {
        // Setup main config
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        LayeredConfig config = new DefaultLayeredConfig();
        config.addListener(listener);
        
        // Add a child
        SettableConfig child = new DefaultSettableConfig();
        child.setProperty("propname", "propvalue");
        config.addConfig(Layers.APPLICATION, child);
        
        // Validate initial state
        assumeTrue("propvalue".equals(config.getProperty("propname").orElse(null)),
                "Property 'propname' does not have the expected value 'propvalue', test assumptions not met.");
        assumeTrue("propvalue".equals(config.getRawProperty("propname")),
                "Raw property 'propname' does not have the expected value 'propvalue', test assumptions not met.");

        // Update the property value
        child.setProperty("propname", "propvalue2");
        
        // Validate new state
        assertEquals("propvalue2", config.getProperty("propname").get());
        assertEquals("propvalue2", config.getRawProperty("propname"));
        
        Mockito.verify(listener, Mockito.times(2)).onConfigUpdated(any());
    }
    
    @Test
    public void validateApiWhenRemovingChild() {
        // Setup main config
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        LayeredConfig config = new DefaultLayeredConfig();
        config.addListener(listener);
        
        // Add a child
        SettableConfig child = new DefaultSettableConfig();
        child.setProperty("propname", "propvalue");
        config.addConfig(Layers.APPLICATION, child);
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(any());
        
        // Validate initial state
        assumeTrue("propvalue".equals(config.getProperty("propname").orElse(null)),
                "Property 'propname' does not have the expected value 'propvalue', test assumptions not met.");
        assumeTrue("propvalue".equals(config.getRawProperty("propname")),
                "Raw property 'propname' does not have the expected value 'propvalue', test assumptions not met.");
        
        // Remove the child
        config.removeConfig(Layers.APPLICATION, child.getName());
        
        // Validate new state
        assertFalse(config.getProperty("propname").isPresent());
        assertNull(config.getRawProperty("propname"));
        
        Mockito.verify(listener, Mockito.times(2)).onConfigUpdated(any());
    }
    
    @Test
    public void validateOverrideOrder() {
        MapConfig appConfig = MapConfig.builder()
                .put("propname", Layers.APPLICATION.getName())
                .name(Layers.APPLICATION.getName())
                .build();
        
        MapConfig lib1Config = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "1")
                .name(Layers.LIBRARY.getName() + "1")
                .build();
        
        MapConfig lib2Config = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "2")
                .name(Layers.LIBRARY.getName() + "2")
                .build();
        MapConfig runtimeConfig = MapConfig.builder()
                .put("propname", Layers.RUNTIME.getName())
                .name(Layers.RUNTIME.getName())
                .build();
        
        LayeredConfig config = new DefaultLayeredConfig();
        assertFalse(config.getProperty("propname").isPresent());

        config.addConfig(Layers.LIBRARY, lib1Config);
        assertEquals(lib1Config.getName(), config.getRawProperty("propname"));
        
        config.addConfig(Layers.LIBRARY, lib2Config);
        assertEquals(lib2Config.getName(), config.getRawProperty("propname"));

        config.addConfig(Layers.RUNTIME, runtimeConfig);
        assertEquals(runtimeConfig.getName(), config.getRawProperty("propname"));
        
        config.addConfig(Layers.APPLICATION, appConfig);
        assertEquals(runtimeConfig.getName(), config.getRawProperty("propname"));
        
        config.removeConfig(Layers.RUNTIME, runtimeConfig.getName());
        assertEquals(appConfig.getName(), config.getRawProperty("propname"));
    }

    @Test
    public void unusedLayeredConfigIsGarbageCollected() {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        LayeredConfig config = new DefaultLayeredConfig();
        config.addConfig(Layers.LIBRARY, sourceConfig);
        Reference<Config> weakReference = new WeakReference<>(config);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        config = null;
        System.gc();
        assertNull(weakReference.get());
    }

    @Test
    public void testGetKeys() {
        LayeredConfig config = new DefaultLayeredConfig();
        MapConfig appConfig = MapConfig.builder()
                .put("propname", Layers.APPLICATION.getName())
                .name(Layers.APPLICATION.getName())
                .build();

        MapConfig libConfig = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "1")
                .name(Layers.LIBRARY.getName() + "1")
                .build();
        config.addConfig(Layers.APPLICATION, appConfig);
        config.addConfig(Layers.LIBRARY, libConfig);

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        assertTrue(keys.hasNext());
        assertEquals("propname", keys.next());
        assertFalse(keys.hasNext());
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() {
        LayeredConfig config = new DefaultLayeredConfig();
        MapConfig appConfig = MapConfig.builder()
                .put("propname", Layers.APPLICATION.getName())
                .name(Layers.APPLICATION.getName())
                .build();

        MapConfig libConfig = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "1")
                .name(Layers.LIBRARY.getName() + "1")
                .build();
        config.addConfig(Layers.APPLICATION, appConfig);
        config.addConfig(Layers.LIBRARY, libConfig);

        @SuppressWarnings("deprecation")
        Iterator<String> keys = config.getKeys();
        assertTrue(keys.hasNext());
        keys.next();
        assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() {
        LayeredConfig config = new DefaultLayeredConfig();
        MapConfig appConfig = MapConfig.builder()
                .put("propname", Layers.APPLICATION.getName())
                .name(Layers.APPLICATION.getName())
                .build();

        MapConfig libConfig = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "1")
                .name(Layers.LIBRARY.getName() + "1")
                .build();
        config.addConfig(Layers.APPLICATION, appConfig);
        config.addConfig(Layers.LIBRARY, libConfig);

        Iterable<String> keys = config.keys();
        assertEquals(1, Iterables.size(keys));
        assertEquals(Collections.singleton("propname"), Sets.newHashSet(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        LayeredConfig config = new DefaultLayeredConfig();
        MapConfig appConfig = MapConfig.builder()
                .put("propname", Layers.APPLICATION.getName())
                .name(Layers.APPLICATION.getName())
                .build();

        MapConfig libConfig = MapConfig.builder()
                .put("propname", Layers.LIBRARY.getName() + "1")
                .name(Layers.LIBRARY.getName() + "1")
                .build();
        config.addConfig(Layers.APPLICATION, appConfig);
        config.addConfig(Layers.LIBRARY, libConfig);

        assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }

    @Test
    public void instrumentationNotEnabled() throws Exception {
        LayeredConfig config = new DefaultLayeredConfig();

        config.addConfig(Layers.DEFAULT, createPollingDynamicConfig("a1", "1", "b1", "2", null));

        assertFalse(config.instrumentationEnabled());
        assertEquals(config.getRawProperty("a1"), "1");
        assertEquals(config.getRawProperty("b1"), "2");
    }

    @Test
    public void instrumentationPropagation() throws Exception {
        com.netflix.archaius.api.config.LayeredConfig layered = new DefaultLayeredConfig();
        AccessMonitorUtil accessMonitorUtil = spy(AccessMonitorUtil.builder().build());

        PollingDynamicConfig outerPollingDynamicConfig = createPollingDynamicConfig("a1", "1", "b1", "2", accessMonitorUtil);
        layered.addConfig(Layers.RUNTIME, outerPollingDynamicConfig);

        com.netflix.archaius.api.config.CompositeConfig innerComposite = new DefaultCompositeConfig();
        PollingDynamicConfig nestedPollingDynamicConfig = createPollingDynamicConfig("b1", "1", "c1", "3", accessMonitorUtil);
        innerComposite.addConfig("polling", nestedPollingDynamicConfig);
        layered.addConfig(Layers.SYSTEM, innerComposite);

        layered.addConfig(Layers.ENVIRONMENT, MapConfig.builder().put("c1", "4").put("d1",  "5").build());

        // Properties (a1: 1) and (b1: 2) are covered by the first polling config
        assertEquals(layered.getRawProperty("a1"), "1");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("a1", "a1", "1")));

        assertEquals(layered.getRawPropertyUninstrumented("a1"), "1");
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        assertEquals(layered.getRawProperty("b1"), "2");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("b1", "b1", "2")));

        assertEquals(layered.getRawPropertyUninstrumented("b1"), "2");
        verify(accessMonitorUtil, times(2)).registerUsage(any());

        // Property (c1: 3) is covered by the composite config over the polling config
        assertEquals(layered.getRawProperty("c1"), "3");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("c1", "c1", "3")));

        assertEquals(layered.getRawPropertyUninstrumented("c1"), "3");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        // Property (d1: 5) is covered by the final, uninstrumented MapConfig
        assertEquals(layered.getRawProperty("d1"), "5");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        assertEquals(layered.getRawPropertyUninstrumented("d1"), "5");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        // The instrumented forEachProperty endpoint updates the counts for every property
        layered.forEachProperty((k, v) -> {});
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("a1", "a1", "1")));
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("b1", "b1", "2")));
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("c1", "c1", "3")));
        verify(accessMonitorUtil, times(6)).registerUsage((any()));

        // The uninstrumented forEachProperty leaves the counts unchanged
        layered.forEachPropertyUninstrumented((k, v) -> {});
        verify(accessMonitorUtil, times(6)).registerUsage((any()));
    }

    private PollingDynamicConfig createPollingDynamicConfig(
            String key1, String value1, String key2, String value2, AccessMonitorUtil accessMonitorUtil) throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Map<String, String> props = new HashMap<>();
        props.put(key1, value1);
        props.put(key2, value2);
        Map<String, String> propIds = new HashMap<>();
        propIds.put(key1, key1);
        propIds.put(key2, key2);
        Callable<PollingResponse> reader = () -> PollingResponse.forSnapshot(props, propIds);
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy, accessMonitorUtil);
        strategy.fire();
        return config;
    }
}
