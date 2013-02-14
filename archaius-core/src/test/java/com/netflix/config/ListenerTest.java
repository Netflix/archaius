package com.netflix.config;

import static org.junit.Assert.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.junit.BeforeClass;
import org.junit.Test;

public class ListenerTest {

	static ConcurrentCompositeConfiguration conf = (ConcurrentCompositeConfiguration) ConfigurationManager.getConfigInstance();
	static ConcurrentMapConfiguration config1 = new ConcurrentMapConfiguration();
	
	static class Listener implements ConfigurationListener {

		volatile ConfigurationEvent lastEventBeforeUpdate;
		
		volatile ConfigurationEvent lastEventAfterUpdate;

		@Override
		public void configurationChanged(ConfigurationEvent event) {
			if (event.isBeforeUpdate()) {
				lastEventBeforeUpdate = event;
			} else {
				lastEventAfterUpdate = event;
			}
		}
		
		public void clear() {
			lastEventBeforeUpdate = null;
			lastEventAfterUpdate = null;
		}
		
		public ConfigurationEvent getLastEvent(boolean beforeUpdate) {
			if (beforeUpdate) {
				return lastEventBeforeUpdate;
			} else {
				return lastEventAfterUpdate;
			}
		}
	}
	
	static Listener listener = new Listener();
	
	@BeforeClass
	public static void init() {
		conf.addConfigurationAtFront(config1, "config1");
		conf.addConfigurationListener(listener);
		listener.clear();
	}
	@Test
	public void testEventsTriggered() {
		config1.addProperty("abc", "foo");
		ConfigurationEvent event = listener.getLastEvent(true);
		assertTrue(event.isBeforeUpdate());
		assertEquals("foo", event.getPropertyValue());
		event = listener.getLastEvent(false);
		assertFalse(event.isBeforeUpdate());
		assertEquals("foo", event.getPropertyValue());
		
	}
}
