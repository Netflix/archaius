package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

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
        
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(Mockito.any());
        
        config.addConfig("bar", DefaultCompositeConfig.builder()
                .withConfig("foo", MapConfig.builder().put("foo.bar", "value").build())
                .build());
        
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(Mockito.any());
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
        Assert.assertEquals("original", privateView.getString("foo.bar"));
        Mockito.verify(listener, Mockito.times(0)).onConfigAdded(Mockito.any());

        // Update the property and confirm onConfigUpdated notification
        settable.setProperty("foo.bar", "new");
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());
        Assert.assertEquals("new", privateView.getString("foo.bar"));
        
        // Add a new config and confirm onConfigAdded notification
        config.addConfig("new", MapConfig.builder().put("foo.bar", "new2").build());
        Mockito.verify(listener, Mockito.times(1)).onConfigAdded(Mockito.any());
        Mockito.verify(listener, Mockito.times(1)).onConfigUpdated(Mockito.any());
    }
}
