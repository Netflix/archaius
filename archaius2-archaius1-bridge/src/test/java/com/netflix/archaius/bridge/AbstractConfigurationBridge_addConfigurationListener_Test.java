package com.netflix.archaius.bridge;

import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.config.DeploymentContext;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;

public class AbstractConfigurationBridge_addConfigurationListener_Test {
    @Test
    public void confirmNotificationsAreBridged() throws IOException, ConfigException {
        SettableConfig settableConfig = new DefaultSettableConfig();
        CompositeConfig librariesConfig = DefaultCompositeConfig.create();
        CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("override", settableConfig)
                .build();
        
        StaticAbstractConfiguration bridge = new StaticAbstractConfiguration(
                config, 
                librariesConfig,
                settableConfig,
                Mockito.mock(DeploymentContext.class)
                );
        
        ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
        bridge.addConfigurationListener(listener);
        
        settableConfig.setProperty("foo", "bar");
        
        ArgumentCaptor<ConfigurationEvent> events = ArgumentCaptor.forClass(ConfigurationEvent.class);
        Mockito.verify(listener, Mockito.times(2)).configurationChanged(events.capture());
    }

}
