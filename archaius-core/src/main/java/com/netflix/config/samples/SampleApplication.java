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
package com.netflix.config.samples;

import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.jmx.ConfigJMXManager;
import com.netflix.config.jmx.ConfigMBean;

/**
 * A Sample Application built to showcase the usage of Configuration and DynamicProperties
 * <p>
 * To run this sample application, add the following jars to your classpath:
 * <ul>
 * <li>archaius-core-xxx.jar (latest release/snapshot of archaius-core)
 * <li>commons-configuration-1.8.jar
 * <li>commons-lang-2.6.jar
 * <li>commons-logging-1.1.1.jar
 * </ul>
 * 
 * @author stonse
 *
 */
public class SampleApplication extends Thread {

	static ConfigMBean configMBean = null;

	public SampleApplication() {
		// This application is set up as a non-daemon app
		// as we want it to just hang around allowing us to launch jconsole
		// to perform properties based operations
		setDaemon(false); 
		start();
	}

	public void run() {
		while (true) {
			try {
				sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
		}
	}
	
	/**
	 * SampleApplication entrypoint
	 * @param args
	 */
	public static void main(String[] args) {
		//@SuppressWarnings(value = {"unused" })
		new SampleApplication();

		// Step 1.
		// Create a Source of Configuration
		// Here we use a simple ConcurrentMapConfiguration
		// You can use your own, or use of the pre built ones including JDBCConfigurationSource
		// which lets you load properties from any RDBMS
		AbstractConfiguration myConfiguration = new ConcurrentMapConfiguration();
		myConfiguration.setProperty("com.netflix.config.samples.sampleapp.prop1", "value1");
		myConfiguration.setProperty(
				"com.netflix.config.samples.SampleApp.SampleBean.name",
				"A Coffee Bean from Gautemala");

		// STEP 2: Optional. Dynamic Runtime property change option
		// We would like to utilize the benefits of obtaining dynamic property
		// changes
		// initialize the DynamicPropertyFactory with our configuration source
		DynamicPropertyFactory.initWithConfigurationSource(myConfiguration);

		// STEP 3: Optional. Option to inspect and modify properties using JConsole
		// We would like to inspect properties via JMX JConsole and potentially
		// update
		// these properties too
		// Register the MBean
		//
		// This can be also achieved automatically by setting "true" to
		// system property "archaius.dynamicPropertyFactory.registerConfigWithJMX"
		configMBean = ConfigJMXManager.registerConfigMbean(myConfiguration);

		// once this application is launched, launch JConsole and navigate to
		// the
		// Config MBean (under the MBeans tab)
		System.out
				.println("Started SampleApplication. Launch JConsole to inspect and update properties");
        System.out.println("To see how callback work, update property com.netflix.config.samples.SampleApp.SampleBean.sensitiveBeanData from BaseConfigBean in JConsole");
		
		SampleBean sampleBean = new SampleBean();
		System.out.println("SampleBean:" + sampleBean);
		System.out.println(sampleBean.getName());

	}

}

/**
 * Simple Class that has its field values influenced (assigned with values) from
 * Properties Some fields can get initialized at startup only and NOT change
 * when actual property value is changed. Others can use
 * <code>DynamicProperty</code> and contain values that are in sync with the
 * runtime changes to the property source.
 * 
 * @author stonse
 * 
 */
class SampleBean {

	/**
	 * Default CTOR
	 */
	public SampleBean() {
		// value of field obtained during init only
		this.name = DynamicPropertyFactory
				.getInstance()
				.getStringProperty(
						"com.netflix.config.samples.SampleApp.SampleBean.name",
						"Sample Bean").get();
	}

	/**
	 * Name of this bean :-) The value for this can be obtained from Properties
	 */
	public String name;

	/**
	 * Number of seeds in this bean. The value for this is automatically
	 * populated by the Configuration. Additionally, if the property value is
	 * modified, the new value will be reflected as well We can assign a default
	 * value as well in case a value was not configured or the configuration was
	 * not successfully loaded
	 * 
	 */
	public DynamicIntProperty numSeeds = DynamicPropertyFactory.getInstance()
			.getIntProperty(
					"com.netflix.config.samples.SampleApp.SampleBean.numSeeds",
					2);
	
	/**
	 * This field utilizes the feature of Callbacks.
	 * When a property value is changed at runtime, it will receive a callback
	 * which can be used to trigger other operations or used for bookkeeping
	 */
	DynamicStringProperty sensitiveBeanData = DynamicPropertyFactory
			.getInstance()
			.getStringProperty(
					"com.netflix.config.samples.SampleApp.SampleBean.sensitiveBeanData",
					"magic", new Runnable() {
						public void run() {
							// let my auditing system know about this change
							System.out
									.println("SampleBean.sensitiveData changed to:"
											+ sensitiveBeanData.get());
						}
					});
	
	/**
	 * returns the name of this bean
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	public String toString() {
	    StringBuilder sb = new StringBuilder();
		sb.append("SampleBean ->name:");
		sb.append(name);
		sb.append("\t numSeeds:");
		sb.append(numSeeds);
		sb.append("\tsensitiveBeanData:");
		sb.append(sensitiveBeanData);
		
		return sb.toString();
	};
	
	
}
