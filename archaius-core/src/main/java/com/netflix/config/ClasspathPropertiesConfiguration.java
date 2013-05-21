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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <i>Conventional Standard</i> based approach to modular configuration
 * 
 * Assuming your application utilizes many modules (.jar files) and need
 * Properties support, this class provides a convention based approach to
 * scanning and loading properties from every jar in a classpath from a specific
 * location. All modules are assumed to have properties in a location such as
 * META-INF/conf/configuration.properties. Such resources may be anywhere in the
 * classpath; for example in directories for a unit test or application.
 * 
 * Parameters from such resources are contained in a Configuration whose name is
 * usually the name of the .jar; for example
 * "com.netflix.movieserviceclient.jar". For a resource not in a .jar, the
 * Configuration name is the URL of the directory that contains the resource.
 * However, if the resource contains a property named conf.configName, its
 * value will be used as the Configuration name.
 * <p>
 * Such properties are often used in Spring, by defining a
 * PropertyPlaceholderConfigurer that gets parameter values from
 * ClasspathConfiguration.getProperties().
 * 
 * @author stonse
 */
public class ClasspathPropertiesConfiguration extends ConcurrentMapConfiguration
{
	private static final Logger log = LoggerFactory
			.getLogger(ClasspathPropertiesConfiguration.class);
	
    static String configNameProperty = "config.configName";
    
    static String propertiesResourceRelativePath = "META-INF/conf/config.properties";
    
    static ClasspathPropertiesConfiguration instance = null;
        
    /** You can't instantiate this class. */
    private ClasspathPropertiesConfiguration()
    {
    	
    }
    
    public String getConfigNameProperty() {
		return configNameProperty;
	}

	/**
	 * Sets the name for the property name that defines the name for a bag of
	 * properties loaded from a properties resources
	 * 
	 * Default if not set is config.configName
	 * 
	 * @param configNameProperty
	 */
	public static void setConfigNameProperty(String configNameProperty) {
		ClasspathPropertiesConfiguration.configNameProperty = configNameProperty;
	}

	public String getPropertiesResourceRelativePath() {
		return propertiesResourceRelativePath;
	}

	/**
	 * 
	 * @param propertiesResourceRelativePath
	 */
	public static void setPropertiesResourceRelativePath(
			String propertiesResourceRelativePath) {
		ClasspathPropertiesConfiguration.propertiesResourceRelativePath = propertiesResourceRelativePath;
	}



	/**
     * Returns properties from this configuration
     */
    public Properties getProperties() 
    {
      	return instance !=null ? instance.getProperties() : new Properties();
    }

    public static void initialize() 
    {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            instance = new ClasspathPropertiesConfiguration();
            loadResources(loader, propertiesResourceRelativePath);
              
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to read configuration properties from classpath", e);
        }
    }

    private static void loadResources(ClassLoader loader,
            String resourceName) throws Exception
    {
        Enumeration<URL> resources = loader.getResources(resourceName);
        boolean loadedResources = false;
		while (resources.hasMoreElements()) {
			URL from = resources.nextElement();
			/* Properties props = loadProperties(from);
			String configName = getConfigName(props, from);
			containerConfiguration.addConfiguration(
					new PropertiesConfiguration(from), configName); */
			ConfigurationManager.loadPropertiesFromResources(from.getPath());
			log.debug("Added properties from:" + from + from.getPath());
			loadedResources = true;
		}
		if (!loadedResources) {
			log.debug("Did not find any properties resource in the classpath with name:"
					+ propertiesResourceRelativePath);
		}
    }

    private static Properties loadProperties(URL from) throws IOException
    {
        Properties into = new Properties();
        InputStream in = from.openStream();
        try {
            into.load(in);
        }catch(Exception e){
        	log.error("Exception while loading properties from URL:" + from);
        } finally {
            in.close();
        }
        return into;
    }

    /**
     * Get the conventional name of the configuration that comes from from the
     * given URL.
     */
    private static String getConfigName(Properties props, URL propertyFile)
    {
        String name = props.getProperty(configNameProperty);
        if (name == null) {
            name = propertyFile.toExternalForm();
            name = name.replace('\\', '/'); // Windows
            final String scheme = propertyFile.getProtocol().toLowerCase();
            if ("jar".equals(scheme) || "zip".equals(scheme)) {
                // Use the unqualified name of the jar file.
                final int bang = name.lastIndexOf("!");
                if (bang >= 0) {
                    name = name.substring(0, bang);
                }
                final int slash = name.lastIndexOf("/");
                if (slash >= 0) {
                    name = name.substring(slash + 1);
                }
            } else {
                // Use the URL of the enclosing directory.
                final int slash = name.lastIndexOf("/");
                if (slash >= 0) {
                    name = name.substring(0, slash);
                }
            }
        }
        return name;
    }



}
