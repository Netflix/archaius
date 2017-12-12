package com.netflix.archaius.config;

import com.netflix.archaius.Layers;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.config.LayeredConfig;
import com.netflix.archaius.api.config.SettableConfig;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLayeredConfigTest {
    @Test
    public void validateApiOnEmptyConfig() {
        LayeredConfig config = new DefaultLayeredConfig();
        
        Assert.assertFalse(config.getProperty("propname").isPresent());
        Assert.assertNull(config.getRawProperty("propname"));
        
        LayeredConfig.LayeredVisitor<String> visitor = Mockito.mock(LayeredConfig.LayeredVisitor.class);
        config.accept(visitor);
        Mockito.verify(visitor, Mockito.never()).visitConfig(Mockito.any(), Mockito.any());
        Mockito.verify(visitor, Mockito.never()).visitKey(Mockito.any(), Mockito.any());
    }
    
    @Test
    public void validateListenerCalled() {
        // Setup main config
        ConfigListener listener = Mockito.mock(ConfigListener.class);
        LayeredConfig config = new DefaultLayeredConfig();
        config.addListener(listener);
        
        // Add a child
        config.addConfig(Layers.APPLICATION, new DefaultSettableConfig());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());
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
        Assert.assertEquals("propvalue", config.getProperty("propname").get());
        Assert.assertEquals("propvalue", config.getRawProperty("propname"));
        
        // Update the property value
        child.setProperty("propname", "propvalue2");
        
        // Validate new state
        Assert.assertEquals("propvalue2", config.getProperty("propname").get());
        Assert.assertEquals("propvalue2", config.getRawProperty("propname"));
        
        Mockito.verify(listener, Mockito.times(2)).onConfigUpdated(Mockito.any());
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
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());        
        
        // Validate initial state
        Assert.assertEquals("propvalue", config.getProperty("propname").get());
        Assert.assertEquals("propvalue", config.getRawProperty("propname"));
        
        // Remove the child
        config.removeConfig(Layers.APPLICATION, child.getName());
        
        // Validate new state
        Assert.assertFalse(config.getProperty("propname").isPresent());
        Assert.assertNull(config.getRawProperty("propname"));
        
        Mockito.verify(listener, Mockito.times(2)).onConfigUpdated(Mockito.any());        
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
        Assert.assertFalse(config.getProperty("propname").isPresent());

        config.addConfig(Layers.LIBRARY, lib1Config);
        Assert.assertEquals(lib1Config.getName(), config.getRawProperty("propname"));
        
        config.addConfig(Layers.LIBRARY, lib2Config);
        Assert.assertEquals(lib2Config.getName(), config.getRawProperty("propname"));

        config.addConfig(Layers.RUNTIME, runtimeConfig);
        Assert.assertEquals(runtimeConfig.getName(), config.getRawProperty("propname"));
        
        config.addConfig(Layers.APPLICATION, appConfig);
        Assert.assertEquals(runtimeConfig.getName(), config.getRawProperty("propname"));
        
        config.removeConfig(Layers.RUNTIME, runtimeConfig.getName());
        Assert.assertEquals(appConfig.getName(), config.getRawProperty("propname"));
    }
}
