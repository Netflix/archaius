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
package com.netflix.config;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * This class uses a ConcurrentHashMap for reading/writing a property to achieve high
 * throughput and thread safety.
 * <p>
 * This configuration does not allow null as key or value and will throw NullPointerException
 * when trying to add or set properties with empty key or value.
 * 
 * @author awang
 *
 */
public class ConcurrentMapConfiguration extends AbstractConfiguration {
    private final Map<String,Object> props = new ConcurrentHashMap<String,Object>();
    
    /**
     * Create an instance with an empty map.
     */
    public ConcurrentMapConfiguration() {
    }
    
    /**
     * Create an instance by copying the properties from an existing Configuration.
     * Future changes to the Configuration passed in will not be reflected in this
     * object.
     * 
     * @param config Configuration to be copied
     */
    public ConcurrentMapConfiguration(Configuration config) {
        for (Iterator i = config.getKeys(); i.hasNext();) {
            String name = (String) i.next();
            Object value = config.getProperty(name);
            addPropertyDirect(name, value);
        }
    }
    
    @Override
    protected void addPropertyDirect(String key, Object value) {
        props.put(key, value);
    }
    
    @Override
    protected void clearPropertyDirect(String key)
    {
       props.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
       return props.containsKey(key);
    }

    @Override
    public Iterator getKeys() {        
        return props.keySet().iterator();
    }

    @Override
    public Object getProperty(String key) {
        return props.get(key);
    }

    @Override
    public boolean isEmpty() { 
        return props.isEmpty();
    }    
    
    /**
     * Load properties into the configuration
     */
    public void loadProperties(Properties props) {
        for (Map.Entry<Object, Object> entry: props.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            setProperty(key, value);
        }
    }
    
    /**
     * Utility method to get a Properties object from this Configuration
     * @return
     */
    public Properties getProperties() {
    	   Properties p = new Properties();
    	   Set<Map.Entry<String, Object>> set = props.entrySet();
    	   for (Map.Entry<String, Object> entry : set) {
    	     p.put(entry.getKey(), entry.getValue());
    	   }
    	   return p;
    }
}
