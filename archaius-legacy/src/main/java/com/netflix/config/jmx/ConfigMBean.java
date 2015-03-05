/**
 * Copyright 2014 Netflix, Inc.
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

/**
 * Configuration MBean Operations are defined in this interface
 * @author stonse
 *
 */
public interface ConfigMBean {

	/**
	 * Returns all Properties. Yes, this should have ideally returned a
	 * <code>Properties</code>, but doing so will make this operation dissapear
	 * from the JConsole.
	 * 
	 */
	public Object obtainProperties();


	/**
	 * Returns the current value of a property given a key
	 * @param key
	 */
	public Object getProperty(String key);

	/**
	 * Adds a new property to the configuration
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value);

	/**
	 * Updates an existing property with the new value
	 * @param key
	 * @param value
	 */
	public void updateProperty(String key, String value);

	/**
	 * Deletes the property identified by the passed in key
	 * @param key
	 */
	public void clearProperty(String key);

}
