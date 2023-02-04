package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void unusedPrefixedViewIsGarbageCollected() {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        Config prefix = sourceConfig.getPrefixedView("foo.");
        Reference<Config> weakReference = new WeakReference<>(prefix);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        prefix = null;
        System.gc();
        Assert.assertNull(weakReference.get());
    }
}
