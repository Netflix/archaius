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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertyConverter;
import org.apache.commons.configuration.event.ConfigurationErrorEvent;
import org.apache.commons.configuration.event.ConfigurationErrorListener;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.validation.ValidationException;

/**
 * This class uses a ConcurrentHashMap for reading/writing a property to achieve high
 * throughput and thread safety. The implementation is lock free for {@link #getProperty(String)}
 * and {@link #setProperty(String, Object)}, but has some synchronization cost for 
 * {@link #addProperty(String, Object)} if the object to add is not a String or the key already exists.
 * <p> 
 * The methods from AbstractConfiguration related to listeners and event generation are overridden
 * so that adding/deleting listeners and firing events are no longer synchronized.
 * Also, it catches Throwable when it invokes the listeners, making
 * it more robust.
 * <p>
 * This configuration does not allow null as key or value and will throw NullPointerException
 * when trying to add or set properties with empty key or value.
 *
 * @author awang
 *
 */
public class ConcurrentMapConfiguration extends AbstractConfiguration {
    protected Map<String,Object> map;
    private Collection<ConfigurationListener> listeners = new CopyOnWriteArrayList<ConfigurationListener>();    
    private Collection<ConfigurationErrorListener> errorListeners = new CopyOnWriteArrayList<ConfigurationErrorListener>();    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMapConfiguration.class);
    private static final int NUM_LOCKS = 32;
    private ReentrantLock[] locks = new ReentrantLock[NUM_LOCKS];

    /**
     * System property to disable delimiter parsing Apache Commons configurations
     */
    public static final String DISABLE_DELIMITER_PARSING = "archaius.configuration.disableDelimiterParsing";

    /**
     * Create an instance with an empty map.
     */
    public ConcurrentMapConfiguration() {
        map = new ConcurrentHashMap<String,Object>();
        for (int i = 0; i < NUM_LOCKS; i++) {
            locks[i] = new ReentrantLock();
        }
        String disableDelimiterParsing = System.getProperty(DISABLE_DELIMITER_PARSING, "false");
        super.setDelimiterParsingDisabled(Boolean.valueOf(disableDelimiterParsing));
    }
    
    public ConcurrentMapConfiguration(Map<String, Object> mapToCopy) {
        this();
        map = new ConcurrentHashMap<String, Object>(mapToCopy);
    }

    /**
     * Create an instance by copying the properties from an existing Configuration.
     * Future changes to the Configuration passed in will not be reflected in this
     * object.
     * 
     * @param config Configuration to be copied
     */
    public ConcurrentMapConfiguration(Configuration config) {
        this();
        for (Iterator i = config.getKeys(); i.hasNext();) {
            String name = (String) i.next();
            Object value = config.getProperty(name);
            map.put(name, value);
        }
    }

    public Object getProperty(String key)
    {
        return map.get(key);
    }

    protected void addPropertyDirect(String key, Object value)
    {
        ReentrantLock lock = locks[Math.abs(key.hashCode()) % NUM_LOCKS];
        lock.lock();
        try {
            Object previousValue = map.putIfAbsent(key, value);
            if (previousValue == null) {
                return;
            }   
            if (previousValue instanceof List)
            {
                // the value is added to the existing list
                ((List) previousValue).add(value);
            }
            else
            {
                // the previous value is replaced by a list containing the previous value and the new value
                List<Object> list = new CopyOnWriteArrayList<Object>();
                list.add(previousValue);
                list.add(value);
                map.put(key, list);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean containsKey(String key)
    {
        return map.containsKey(key);
    }

    protected void clearPropertyDirect(String key)
    {
        map.remove(key);
    }

    public Iterator getKeys()
    {
        return map.keySet().iterator();
    }
    

    /**
     * Adds the specified value for the given property. This method supports
     * single values and containers (e.g. collections or arrays) as well. In the
     * latter case, {@link #addPropertyDirect(String, Object)} will be called for each
     * element.
     *
     * @param key the property key
     * @param value the value object
     * @param delimiter the list delimiter character
     */
    private void addPropertyValues(String key, Object value, char delimiter)
    {
        Iterator it = PropertyConverter.toIterator(value, delimiter);
        while (it.hasNext())
        {
            addPropertyDirect(key, it.next());
        }
    }

    public void addProperty(String key, Object value) throws ValidationException
    {
        if (value == null) {
            throw new NullPointerException("Value for property " + key + " is null");
        }
        fireEvent(EVENT_ADD_PROPERTY, key, value, true);
        addPropertyImpl(key, value);
        fireEvent(EVENT_ADD_PROPERTY, key, value, false);
    }

    protected void addPropertyImpl(String key, Object value) {
        Object previousValue = null;
        if (isDelimiterParsingDisabled() ||
                ((value instanceof String) && ((String) value).indexOf(getListDelimiter()) < 0)) {
            previousValue = map.putIfAbsent(key, value);
            if (previousValue != null) {
                addPropertyValues(key, value,
                        isDelimiterParsingDisabled() ? '\0'
                                : getListDelimiter());
            }
        } else {
            addPropertyValues(key, value,
                    isDelimiterParsingDisabled() ? '\0'
                            : getListDelimiter());
            
        }
    }
    
    /**
     * Override the same method in {@link AbstractConfiguration} to simplify the logic
     * to avoid multiple events being generated. It calls {@link #clearPropertyDirect(String)}
     * followed by logic to add the property including calling {@link #addPropertyDirect(String, Object)}. 
     */
    @Override
    public void setProperty(String key, Object value) throws ValidationException
    {
        if (value == null) {
            throw new NullPointerException("Value for property " + key + " is null");
        }
        fireEvent(EVENT_SET_PROPERTY, key, value, true);
        setPropertyImpl(key, value);
        fireEvent(EVENT_SET_PROPERTY, key, value, false);
    }
    
    protected void setPropertyImpl(String key, Object value) {
        if (isDelimiterParsingDisabled()) {
            map.put(key, value);
        } else if ((value instanceof String) && ((String) value).indexOf(getListDelimiter()) < 0) {
            map.put(key, value);
        } else {
            Iterator it = PropertyConverter.toIterator(value, getListDelimiter());
            List<Object> list = new CopyOnWriteArrayList<Object>();
            while (it.hasNext())
            {
                list.add(it.next());
            }
            if (list.size() == 1) {
                map.put(key, list.get(0));
            } else {
                map.put(key, list);
            }
        }        
    }
    
    /**
     * Load properties into the configuration. This method iterates through
     * the entries of the properties and call {@link #setProperty(String, Object)} for 
     * non-null key/value.
     */
    public void loadProperties(Properties props) {
        for (Map.Entry<Object, Object> entry: props.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (key != null && value != null) {
                setProperty(key, value);
            }
        }
    }
    
    /**
     * Copy properties of a configuration into this configuration. This method simply
     * iterates the keys of the configuration to be copied and call {@link #setProperty(String, Object)}
     * for non-null key/value.
     */
    @Override
    public void copy(Configuration c)
    {
        if (c != null)
        {
            for (Iterator it = c.getKeys(); it.hasNext();)
            {
                String key = (String) it.next();
                Object value = c.getProperty(key);
                if (key != null && value != null) {
                    setProperty(key, value);
                }
            }
        }
    }

    /**
     * Clear the map and fire corresonding events.
     */
    @Override
    public void clear()
    {
        fireEvent(EVENT_CLEAR, null, null, true);
        map.clear();
        fireEvent(EVENT_CLEAR, null, null, false);
    }

    /**
     * Utility method to get a Properties object from this Configuration
     */
    public Properties getProperties() {
        Properties p = new Properties();
        for (Iterator i = getKeys(); i.hasNext();) {
            String name = (String) i.next();
            String value = getString(name);
            p.put(name, value);
        }
        return p;
    }
    
    /**
     * Creates an event and calls {@link ConfigurationListener#configurationChanged(ConfigurationEvent)}
     * for all listeners while catching Throwable.
     */
    @Override
    protected void fireEvent(int type, String propName, Object propValue, boolean beforeUpdate) {
        if (listeners == null || listeners.size() == 0) {
            return;
        }        
        ConfigurationEvent event = createEvent(type, propName, propValue, beforeUpdate);
        for (ConfigurationListener l: listeners)
        {
            try {
                l.configurationChanged(event);
            } catch (ValidationException e) {
                if (beforeUpdate) {
                    throw e;
                } else {
                    logger.error("Unexpected exception", e);                    
                }
            } catch (Throwable e) {
                logger.error("Error firing configuration event", e);
            }
        }
    }
    
    @Override
    public void addConfigurationListener(ConfigurationListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }


    @Override
    public void addErrorListener(ConfigurationErrorListener l) {
        if (!errorListeners.contains(l)) {
            errorListeners.add(l);
        }
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
    public boolean removeConfigurationListener(ConfigurationListener l) {
        return listeners.remove(l);
    }


    @Override
    public boolean removeErrorListener(ConfigurationErrorListener l) {
        return errorListeners.remove(l);
    }

    /**
     * Creates an error event and calls {@link ConfigurationErrorListener#configurationError(ConfigurationErrorEvent)}
     * for all listeners while catching Throwable.
     */
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
