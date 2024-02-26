package com.netflix.archaius.bridge;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.config.ConfigurationManager;

import javax.inject.Inject;

public class StaticBridgeAddConfigurationTest {
	private static ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
	
	public static class Foo {
		public static void addListenerBeforeBridgeInitialization() {
			ConfigurationManager.getConfigInstance().addConfigurationListener(listener);
		}
	}
	
	@Inject
	@RuntimeLayer
	SettableConfig settableConfig;
	
	@Test
	public void listenerAddedToStaticBeforeStaticInjection() {
		Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule(), new AbstractModule() {
			@Override
			protected void configure() {
				Foo.addListenerBeforeBridgeInitialization();
				this.requestInjection(StaticBridgeAddConfigurationTest.this);
			}
		});

		// Verify that the listener is called
		Mockito.verify(listener, Mockito.never()).configurationChanged(Mockito.isA(ConfigurationEvent.class));
		settableConfig.setProperty("foo", "bar");
		Mockito.verify(listener, Mockito.times(2)).configurationChanged(Mockito.isA(ConfigurationEvent.class));
		
		// Listener no longer invoked after reset
		StaticArchaiusBridgeModule.resetStaticBridges();
		settableConfig.setProperty("bar", "baz");
		Mockito.verify(listener, Mockito.times(2)).configurationChanged(Mockito.isA(ConfigurationEvent.class));
		
	}
}
