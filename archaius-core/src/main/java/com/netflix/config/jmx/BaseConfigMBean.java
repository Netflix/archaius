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
package com.netflix.config.jmx;

import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.util.ConfigurationUtils;

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
		return ConfigurationUtils.getProperties(config);
	}

	@Override
	public Object getProperty(String key) {
		return config.getProperty(key);
	}

	/**
	 * Calls <code>config.setProperty()</code>. If the underlying configuration
	 * is {@link ConcurrentCompositeConfiguration}, it calls {@link ConcurrentCompositeConfiguration#setOverrideProperty(String, Object)}
	 * instead.
	 */
	@Override
	public void updateProperty(String key, String value) {
	    if (config instanceof ConcurrentCompositeConfiguration) {
	        ((ConcurrentCompositeConfiguration) config).setOverrideProperty(key, value);
	    } else {
		    config.setProperty(key, value);
	    }
	}

    /**
     * Calls <code>config.clearProperty()</code>. If the underlying configuration
     * is {@link ConcurrentCompositeConfiguration}, it calls {@link ConcurrentCompositeConfiguration#clearOverrideProperty(String)}
     * instead. 
     * <p><b>Warning: </b>{@link ConcurrentCompositeConfiguration#clearOverrideProperty(String)} does not clear the 
     * property with the whole {@link ConcurrentCompositeConfiguration}, if any other child configurations in it has the same property.
     */
	@Override
	public void clearProperty(String key) {
        if (config instanceof ConcurrentCompositeConfiguration) {
            ((ConcurrentCompositeConfiguration) config).clearOverrideProperty(key);
        } else {
		    config.clearProperty(key);
        }
	}

    /**
     * Calls <code>config.addrProperty()</code>. If the underlying configuration
     * is {@link ConcurrentCompositeConfiguration}, it calls {@link ConcurrentCompositeConfiguration#setOverrideProperty(String, Object)}
     * instead.
     */
	@Override
	public void addProperty(String key, String value) {
        if (config instanceof ConcurrentCompositeConfiguration) {
            ((ConcurrentCompositeConfiguration) config).setOverrideProperty(key, value);
        } else {
		    config.addProperty(key, value);
        }
	}

}
