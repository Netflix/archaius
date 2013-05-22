/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
