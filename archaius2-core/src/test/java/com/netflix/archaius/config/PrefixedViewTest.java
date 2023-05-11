package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static com.netflix.archaius.TestUtils.set;
import static com.netflix.archaius.TestUtils.size;

public class PrefixedViewTest {
    @Test
    public void confirmNotifactionOnAnyChange() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build();
        
        Config prefix = config.getPrefixedView("foo");
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        
        prefix.addListener(listener);
        
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(Mockito.any());
        
        config.addConfig("bar", DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(Mockito.any());
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
        Assert.assertEquals("original", prefix.getString("bar"));
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(Mockito.any());

        // Update the property and confirm onConfigUpdated notification
        settable.setProperty("foo.bar", "new");
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());
        Assert.assertEquals("new", prefix.getString("bar"));
        
        // Add a new config and confirm onConfigAdded notification
        config.addConfig("new", MapConfig.builder().put("foo.bar", "new2").build());
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(Mockito.any());
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());
    }

    @Test
    public void trailingDotAllowed() {
        SettableConfig settable = new DefaultSettableConfig();
        settable.setProperty("foo.bar", "value");

        Config prefixNoDot = settable.getPrefixedView("foo");
        Config prefixWithDot = settable.getPrefixedView("foo.");

        Assert.assertEquals(prefixNoDot.getString("bar"), "value");
        Assert.assertEquals(prefixWithDot.getString("bar"), "value");
    }

    @Test
    public void unusedPrefixedViewIsGarbageCollected() {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        Config prefix = sourceConfig.getPrefixedView("foo.");
        Reference<Config> weakReference = new WeakReference<>(prefix);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        prefix = null;
        System.gc();
        Assert.assertNull(weakReference.get());
    }

    @Test
    public void testGetKeys() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        Iterator<String> keys = config.getKeys();
        Set<String> keySet = new HashSet<>();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }
        Assert.assertEquals(2, keySet.size());
        Assert.assertTrue(keySet.contains("prop1"));
        Assert.assertTrue(keySet.contains("prop2"));
    }

    @Test
    public void testGetKeysIteratorRemoveThrows() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        Iterator<String> keys = config.getKeys();
        keys.next();
        Assert.assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        Iterable<String> keys = config.keys();
        Assert.assertEquals(2, size(keys));
        Assert.assertEquals(set("prop1", "prop2"), set(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() {
        Config config = MapConfig.builder()
                .put("foo.prop1", "value1")
                .put("foo.prop2", "value2")
                .build()
                .getPrefixedView("foo");

        Assert.assertThrows(UnsupportedOperationException.class, config.keys().iterator()::remove);
        Assert.assertThrows(UnsupportedOperationException.class, ((Collection<String>) config.keys())::clear);
    }
}
