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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.event.ConfigurationErrorEvent;
import org.apache.commons.configuration.event.ConfigurationErrorListener;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class uses a ConcurrentHashMap for reading/writing a property to achieve high
 * throughput and thread safety. 
 * The methods from AbstractConfiguration related to listeners and event generation are rewritten
 * so that adding/deleting listeners and firing events are no longer synchronized. Therefore,
 * code in this class is lock free. Also, it catches Throwable when it invokes the listeners, making
 * it more robust.
 * <p>
 * This configuration does not allow null as key or value and will throw NullPointerException
 * when trying to add or set properties with empty key or value.
 * 
 * @author awang
 *
 */
public class ConcurrentMapConfiguration extends AbstractConfiguration {
    private final Map<String,Object> props = new ConcurrentHashMap<String,Object>();
    private Collection<ConfigurationListener> listeners = new CopyOnWriteArrayList<ConfigurationListener>();    
    private Collection<ConfigurationErrorListener> errorListeners = new CopyOnWriteArrayList<ConfigurationErrorListener>();    
    private AtomicLong detailEventsCount = new AtomicLong();    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMapConfiguration.class);
    
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
    
    @Override
    protected void fireEvent(int type, String propName, Object propValue, boolean beforeUpdate) {
        if (listeners == null || listeners.size() == 0 || detailEventsCount.get() < 0) {
            return;
        }        
        ConfigurationEvent event = createEvent(type, propName, propValue, beforeUpdate);
        for (ConfigurationListener l: listeners)
        {
            try {
                l.configurationChanged(event);
            } catch (Throwable e) {
                logger.error("Error firing configuration event", e);
            }
        }
    }
    
    @Override
    public void addConfigurationListener(ConfigurationListener l) {
        listeners.add(l);
    }


    @Override
    public void addErrorListener(ConfigurationErrorListener l) {
        errorListeners.add(l);
    }


    @Override
    public void clearConfigurationListeners() {
        listeners.clear();
    }


    @Override
    public void clearErrorListeners() {
        errorListeners.clear();
    }


    @Override
    public Collection<ConfigurationListener> getConfigurationListeners() {
        return Collections.unmodifiableCollection(listeners);
    }


    @Override
    public Collection<ConfigurationErrorListener> getErrorListeners() {
        return Collections.unmodifiableCollection(errorListeners);
    }

    @Override
    public boolean isDetailEvents() {
        return detailEventsCount.get() > 0;    
    }


    @Override
    public boolean removeConfigurationListener(ConfigurationListener l) {
        return listeners.remove(l);
    }


    @Override
    public boolean removeErrorListener(ConfigurationErrorListener l) {
        return errorListeners.remove(l);
    }


    @Override
    public void setDetailEvents(boolean enable) {
        if (enable) {
            detailEventsCount.incrementAndGet();
        } else {
            detailEventsCount.decrementAndGet();
        }
    }

    @Override
    protected void fireError(int type, String propName, Object propValue, Throwable ex)
    {
        if (errorListeners == null || errorListeners.size() == 0) {
            return;
        }

        ConfigurationErrorEvent event = createErrorEvent(type, propName, propValue, ex);
        for (ConfigurationErrorListener l: errorListeners) {
            try {
                l.configurationError(event);
            } catch (Throwable e) {
                logger.error("Error firing configuration error event", e);
            }
        }
     }
    

}
