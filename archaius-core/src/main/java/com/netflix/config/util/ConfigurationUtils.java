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
package com.netflix.config.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;


/**
 * Utility class for configuration.
 * 
 * @author awang
 *
 */
public class ConfigurationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);
    /**
     * Convert CombinedConfiguration into {@link ConcurrentCompositeConfiguration} as the later has better performance
     * and thread safety. 
     * 
     * @param config Configuration to be converted
     */
    public static ConcurrentCompositeConfiguration convertToConcurrentCompositeConfiguration(CombinedConfiguration config) {
        ConcurrentCompositeConfiguration root = new ConcurrentCompositeConfiguration();
        IdentityHashMap<Configuration, String> reverseMap = new IdentityHashMap<Configuration, String>();
        for (String name: (Set<String>) config.getConfigurationNames()) {
            Configuration child = config.getConfiguration(name);
            reverseMap.put(child, name);
        } 
        for (int i = 0; i < config.getNumberOfConfigurations(); i++) {
            Configuration child = config.getConfiguration(i);
            String name = reverseMap.get(child);
            if (child instanceof CombinedConfiguration) {
                CombinedConfiguration combinedConf = (CombinedConfiguration) child;
                ConcurrentCompositeConfiguration newConf = convertToConcurrentCompositeConfiguration(combinedConf);
                root.addConfiguration(newConf, name);
            } else {
                Configuration conf = new ConcurrentMapConfiguration(child);
                root.addConfiguration((AbstractConfiguration) conf, name);
            }
        }
        return root;
    }
    
    /**
     * Gets all named sub-configuration from a configuration in a map. This method examines each sub-configuration
     * which is an instance of {@link ConcurrentCompositeConfiguration} or CombinedConfiguration and extract the 
     * named configurations out of them.
     *  
     * @param conf Configuration to get all the named sub-configurations
     * @return map where key is the name of the sub-configuration and value is the sub-configuration
     */
    public static Map<String, Configuration> getAllNamedConfiguration(Configuration conf) {
        List<Configuration> toProcess = new ArrayList<Configuration>();
        Map<String, Configuration> map = new HashMap<String, Configuration>();
        toProcess.add(conf);
        while (!toProcess.isEmpty()) {
            Configuration current = toProcess.remove(0);
            if (current instanceof ConcurrentCompositeConfiguration) {
                ConcurrentCompositeConfiguration composite = (ConcurrentCompositeConfiguration) current;
                for (String name: composite.getConfigurationNames()) {
                    map.put(name, composite.getConfiguration(name));
                }
                for (Configuration c: composite.getConfigurations()) {
                    toProcess.add(c);
                }
            } else if (current instanceof CombinedConfiguration) {
                CombinedConfiguration combined = (CombinedConfiguration) current;
                for (String name: (Set<String>) combined.getConfigurationNames()) {
                    map.put(name, combined.getConfiguration(name));
                }
                for (int i = 0; i < combined.getNumberOfConfigurations(); i++) {
                    toProcess.add(combined.getConfiguration(i));
                }
            }
        }
        return map;
    }
    
    /**
     * Utility method to obtain <code>Properties</code> given an instance of <code>AbstractConfiguration</code>.
     * Returns an empty <code>Properties</code> object if the config has no properties or is null.
     * @param config Configuration to get the properties
     * @return properties extracted from the configuration
     */
    public static Properties getProperties(Configuration config) {
 	   Properties p = new Properties();
 	   if (config != null){
	 	   Iterator<String> it = config.getKeys();	 	   
	 	   while (it.hasNext()){
	 		   String key = it.next();
	 		   if (key != null) {
	 		      Object value = config.getProperty(key);
                  if (value != null) {
                      p.put(key, value);
                  }	 		   }
	 	   }
 	   }
  	   return p;
    }
    
    public static void loadProperties(Properties props, Configuration config) {
        for (Entry<Object, Object> entry: props.entrySet()) {
            config.setProperty((String) entry.getKey(), entry.getValue());
        }
    }
    
    static void loadFromPropertiesFile(AbstractConfiguration config, String baseUrl, Set<String> loaded, String... nextLoadKeys) {
        String nextLoad = getNextLoad(config, nextLoadKeys);
        if (nextLoad == null) {
            return;
        }
        String[] filesToLoad = nextLoad.split(",");
        for (String fileName: filesToLoad) {
            fileName = fileName.trim();
            try {
                URL url = new URL(baseUrl + "/" + fileName);
                // avoid circle
                if (loaded.contains(url.toExternalForm())) {
                    logger.warn(url + " is already loaded");
                    continue;
                }
                loaded.add(url.toExternalForm());
                PropertiesConfiguration nextConfig = new OverridingPropertiesConfiguration(url);
                copyProperties(nextConfig, config);
                logger.info("Loaded properties file " + url);
                loadFromPropertiesFile(config, baseUrl, loaded, nextLoadKeys);
            } catch (Throwable e) {
                logger.warn("Unable to load properties file", e);
            }
        }
    }
    
    
    public static AbstractConfiguration getConfigFromPropertiesFile(URL startingUrl, Set<String> loaded, String... nextLoadKeys) 
            throws FileNotFoundException {
        if (loaded.contains(startingUrl.toExternalForm())) {
            logger.warn(startingUrl + " is already loaded");
            return null;
        }
        PropertiesConfiguration propConfig = null;
        try {
            propConfig = new OverridingPropertiesConfiguration(startingUrl);
            logger.info("Loaded properties file " + startingUrl);            
        } catch (ConfigurationException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FileNotFoundException) {
                throw (FileNotFoundException) cause;
            } else {
                throw new RuntimeException(e);
            }
        }
        
        if (nextLoadKeys == null) {
            return propConfig;
        }
        String urlString = startingUrl.toExternalForm();
        String base = urlString.substring(0, urlString.lastIndexOf("/"));
        loaded.add(startingUrl.toString());
        loadFromPropertiesFile(propConfig, base, loaded, nextLoadKeys);
        return propConfig;
    }
    
    public static void copyProperties(Configuration from, Configuration to) {
        for (Iterator<String> i = from.getKeys(); i.hasNext(); ) {
            String key = i.next();
            if (key != null) {                
                Object value = from.getProperty(key);
                if (value != null) {
                    to.setProperty(key, value);
                }
            }
        }        
    }
    
    public static Properties getPropertiesFromFile(URL startingUrl, Set<String> loaded, String... nextLoadKeys) 
            throws FileNotFoundException {
        AbstractConfiguration config = getConfigFromPropertiesFile(startingUrl, loaded, nextLoadKeys);
        return getProperties(config);
    }


    private static String getNextLoad(Configuration propConfig, String... nextLoadPropertyKeys) {
        String nextLoadKeyToUse = null;
        for (String key: nextLoadPropertyKeys) {
            if (propConfig.getProperty(key) != null) {
                nextLoadKeyToUse = key;
                break;
            }
        }
        // there is no next load for this properties file
        if (nextLoadKeyToUse == null) {
            return null;
        }
        // make a copy of current existing properties
        ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
        
        // need to have all the properties to interpolate next load property value
        copyProperties(ConfigurationManager.getConfigInstance(), config);
        copyProperties(propConfig, config);
        // In case this is a list of files to load, always treat the value as a list
        List<Object> list = config.getList(nextLoadKeyToUse);
        StringBuilder sb = new StringBuilder();
        for (Object value: list) {
            sb.append(value).append(",");
        }
        String nextLoad = sb.toString();
        propConfig.clearProperty(nextLoadKeyToUse);
        return nextLoad;
    }
    
    /**
     * Load properties from InputStream with utf-8 encoding, and it will take care of closing the input stream.
     * @param fin
     * @return
     * @throws IOException
     */
    public static Properties loadPropertiesFromInputStream(InputStream fin) throws IOException {
        Properties props = new Properties();
        InputStreamReader reader = new InputStreamReader(fin, "UTF-8");
        try {
            props.load(reader);
            return props;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (fin != null) {
                fin.close();
            }
        }
    }
}

class OverridingPropertiesConfiguration extends PropertiesConfiguration {

    public OverridingPropertiesConfiguration() {
        super();
    }

    public OverridingPropertiesConfiguration(File file)
            throws ConfigurationException {
        super(file);
    }

    public OverridingPropertiesConfiguration(String fileName)
            throws ConfigurationException {
        super(fileName);
    }

    public OverridingPropertiesConfiguration(URL url)
            throws ConfigurationException {
        super(url);
    }
    
    /**
     * Need to override this method for PDCLOUD-1809.
     * 
     */
    @Override
    public void addProperty(String name, Object value) {
        if (containsKey(name)) {
            // clear without triggering an event
            clearPropertyDirect(name);
        }
        super.addProperty(name, value);        
    }    
}
