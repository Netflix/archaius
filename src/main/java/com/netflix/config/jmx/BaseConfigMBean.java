/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config.jmx;

import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;

/**
 * A basic implementation of a Config MBean that allows for operations on
 * properties contained in the <code>AbstractConfiguration</code>.
 * 
 * @author stonse
 * 
 */
public class BaseConfigMBean implements ConfigMBean {

	AbstractConfiguration config = null;

	public BaseConfigMBean(AbstractConfiguration config) {
		this.config = config;
	}
	
	@Override
	public Object obtainProperties() {
		return new Properties();
	}

	@Override
	public Object getProperty(String key) {
		return config.getProperty(key);
	}

	@Override
	public void updateProperty(String key, String value) {
		config.setProperty(key, value);
	}

	@Override
	public void clearProperty(String key) {
		config.clearProperty(key);
	}

	@Override
	public void addProperty(String key, String value) {
		config.addProperty(key, value);
	}

}
