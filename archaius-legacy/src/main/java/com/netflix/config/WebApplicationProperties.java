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
package com.netflix.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.util.ConfigurationUtils;

import com.netflix.config.sources.URLConfigurationSource;

/**
 * This class helps in loading properties present in a typical web appplication.
 * 
 * 1. In a series of application properties file (typically under
 * WEB-INF/conf/*) 
 * 
 * 2. Every library (JAR) located in the web application's
 * classpath i.e. WEB-INF/lib/*.jar, can contain properties in say
 * META-INF/conf/config.properties
 * 
 * This class helps load all the properties into the Default Configuration.
 * 
 * In case you would like to load these properties into another Configuration
 * instead, you can use {@link #getProperties()} to obtain the properties and
 * load these into the <code>Configuration</code> of your choice.
 * 
 * @author stonse
 * 
 */
public class WebApplicationProperties {

	private static final Logger logger = LoggerFactory
			.getLogger(WebApplicationProperties.class);

	static File appConfFolder = new File("."); // folder where the
												// application properties
												// files are
	static String baseConfigFileName = "config";
	
	static boolean loadLibraryProperties = true;
	
	static String libraryPropertiesResourceRelativePath = ClasspathPropertiesConfiguration.propertiesResourceRelativePath;

	/**
	 * Initialize. The application should call this method once the Application
	 * Folder is set using {@link #setAppConfFolder(File, String)} method.
	 * 
	 * @param appConfFolderFromConfig
	 * @param baseConfigFileName
	 * @param loadLibraryProperties
	 * @param propertiesResourceRelativePath
	 */
	public static void initialize(File appConfFolderFromConfig,
			String baseConfigFileName, boolean loadLibraryProperties,
			String propertiesResourceRelativePath) {
		WebApplicationProperties.appConfFolder = appConfFolderFromConfig;
		WebApplicationProperties.baseConfigFileName = baseConfigFileName;
		WebApplicationProperties.loadLibraryProperties = loadLibraryProperties;
		WebApplicationProperties.libraryPropertiesResourceRelativePath = propertiesResourceRelativePath;
		initialize();
	}
	
	/**
	 * Initialize.
	 * The application should call this method once the Application Folder is set using {@link #setAppConfFolder(File, String)} method.
	 */
	public static void initialize() {
		try {
			initApplicationProperties();
		} catch (Exception e) {
			logger.error("Unable to load Application Properties", e);
			System.err.println("Unable to load Application Properties");
			e.printStackTrace();
		}

		// should we load the properties in the JAR files?
		if (loadLibraryProperties){
			try {
				initClasspathPropertiesConfiguration();
			} catch (Exception e) {
				logger.error("Unable to load Library Properties", e);
				System.err.println("Unable to load Library Properties");
				e.printStackTrace();
			}
		}

		logger.debug("Properties loaded:" + getProperties());
	}

	protected static void initClasspathPropertiesConfiguration() {
		ClasspathPropertiesConfiguration.initialize();
	}

	protected static void initApplicationProperties()
			throws ConfigurationException, MalformedURLException {
		File appPropFile = new File(appConfFolder + "/" + baseConfigFileName + ".properties");
		File appEnvPropOverrideFile = new File(appConfFolder + "/" + baseConfigFileName +
		 getEnvironment() + ".properties");
		
		 
		// TODO awang, how do we add this to archaius default config?
		PropertiesConfiguration appConf = new PropertiesConfiguration(
				appPropFile);
		// apply env overrides
		PropertiesConfiguration overrideConf = new PropertiesConfiguration(appEnvPropOverrideFile);
		Properties overrideprops = ConfigurationUtils.getProperties(overrideConf);
		for (Object prop: overrideprops.keySet()){
			appConf.setProperty(""+prop, overrideprops.getProperty(""+prop));
		}
		String path = appPropFile.toURI().toURL().toString();
		System.setProperty(URLConfigurationSource.CONFIG_URL, path);
		ConfigurationManager.loadPropertiesFromConfiguration(appConf);

	}

	private static String getEnvironment() {
		String env = System.getProperty("env");
		return env;
	}

	public static File getAppConfFolder() {
		return appConfFolder;
	}

	/**
	 * The folder where the application's properties files are located.
	 * 
	 * @param appConfFolderFromConfig
	 *            the folder as a <code>File</code> object where the
	 *            application's properties files are located
	 * @param baseConfigFileName
	 *            the "base" name of the properties file. For e.g. if you have
	 *            config.properties, config-test.properties, then the value to
	 *            pass in is "config"
	 */
	public static void setAppConfFolder(File appConfFolderFromConfig, String baseConfigFileName) {
		WebApplicationProperties.appConfFolder = appConfFolderFromConfig;
		WebApplicationProperties.baseConfigFileName = baseConfigFileName;
	}
	
	
	/**
	 * Should we load the Library Properties
	 */
	public static boolean shouldLoadLibraryProperties() {
		return loadLibraryProperties;
	}

	/**
	 * Set this if you would like the {@link ClasspathPropertiesConfiguration}
	 * to scan the JAR files in the classpath and load those proerties into the
	 * default Configuration
	 * 
	 * @param loadLibraryProperties
	 */
	public static void setLoadLibraryProperties(boolean loadLibraryProperties) {
		WebApplicationProperties.loadLibraryProperties = loadLibraryProperties;
	}

	/**
	 * Returns the relative Resource Path of the properties files in the JAR files
	 */
	public static String getLibraryPropertiesResourceRelativePath() {
		return libraryPropertiesResourceRelativePath;
	}

	/**
	 * Please see {@link ClasspathPropertiesConfiguration} on Library Properties.
	 * This methods sets the relativeResource path
	 * @param libraryPropertiesResourceRelativePath
	 */
	public static void setLibraryPropertiesResourceRelativePath(
			String libraryPropertiesResourceRelativePath) {
		WebApplicationProperties.libraryPropertiesResourceRelativePath = libraryPropertiesResourceRelativePath;
	}

	/**
	 * Returns all the properties presently available
	 */
	public static Properties getProperties() {
		return ConfigurationUtils.getProperties(ConfigurationManager.getConfigInstance());
	}
}
