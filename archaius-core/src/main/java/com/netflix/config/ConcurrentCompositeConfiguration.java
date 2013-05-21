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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.tree.OverrideCombiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class maintains a hierarchy of configurations in a list structure. The order of the list stands for the descending
 * priority of the configurations when a property value is to be determined.
 * For example, if you add Configuration1, and then Configuration2,
 * {@link #getProperty(String)} will return any properties defined by Configuration1.
 * Only if Configuration1 doesn't have the property, then
 * Configuration2 will be checked. </p>
 * There are two internal configurations for properties that are programmatically set:
 * <ul>
 * <li>Configuration to hold any property introduced by {@link #addProperty(String, Object)} or {@link #setProperty(String, Object)}
 * called directly on this class. This configuration will be called "container configuration" as it serves as the container of
 * such properties. By default, this configuration remains at the last of the configurations list. It can be treated as 
 * a "base line" configuration that holds hard-coded parameters that can be overridden by any of other configurations added at runtime. 
 * You can replace this configuration by your own and change the position of the configuration in the list by calling
 * {@link #setContainerConfiguration(AbstractConfiguration, String, int)}. 
 * <li>Configuration to hold properties that are programmatically set (using {@link #setOverrideProperty(String, Object)}) to override values from any other 
 * configurations on the list. As contrast to container configuration, this configuration is always consulted first in 
 * {@link #getProperty(String)}. 
 * </ul>
 * 
 * When adding configuration to this class, it is recommended to convert it into
 * {@link ConcurrentMapConfiguration} or ConcurrentCompositeConfiguration using 
 * {@link com.netflix.config.util.ConfigurationUtils} to achieve
 * maximal performance and thread safety.
 * 
 * <p>
 * Example:
 * <pre>
 *   // configuration from local properties file
  String fileName = "...";
  ConcurrentMapConfiguration configFromPropertiesFile =
      new ConcurrentMapConfiguration(new PropertiesConfiguration(fileName));
  // configuration from system properties
  ConcurrentMapConfiguration configFromSystemProperties = 
      new ConcurrentMapConfiguration(new SystemConfiguration());
  // configuration from a dynamic source
  PolledConfigurationSource source = createMyOwnSource();
  AbstractPollingScheduler scheduler = createMyOwnScheduler();
  DynamicConfiguration dynamicConfiguration =
      new DynamicConfiguration(source, scheduler);
  
  // create a hierarchy of configuration that makes
  // 1) dynamic configuration source override system properties and,
  // 2) system properties override properties file
  ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();
  finalConfig.add(dynamicConfiguration, "dynamicConfig");
  finalConfig.add(configFromSystemProperties, "systemConfig");
  finalConfig.add(configFromPropertiesFile, "fileConfig");

  // register with DynamicPropertyFactory so that finalConfig
  // becomes the source of dynamic properties
  DynamicPropertyFactory.initWithConfigurationSource(finalConfig);    
 * </pre>
 * 
 * @author awang
 *
 */
public class ConcurrentCompositeConfiguration extends ConcurrentMapConfiguration 
        implements AggregatedConfiguration, ConfigurationListener, Cloneable {

    private Map<String, AbstractConfiguration> namedConfigurations = new ConcurrentHashMap<String, AbstractConfiguration>();
    
    private List<AbstractConfiguration> configList = new CopyOnWriteArrayList<AbstractConfiguration>();
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentCompositeConfiguration.class);
    
    public static final int EVENT_CONFIGURATION_SOURCE_CHANGED = 10001;
    
    private volatile boolean propagateEventToParent = true;
    
    private AbstractConfiguration overrideProperties;
        
    /**
     * Configuration that holds properties set directly with {@link #setProperty(String, Object)}
     */
    private AbstractConfiguration containerConfiguration;

    /**
     * Stores a flag whether the current in-memory configuration is also a
     * child configuration.
     */
    private volatile boolean containerConfigurationChanged = true;

    private ConfigurationListener eventPropagater = new ConfigurationListener() {
        @Override
        public void configurationChanged(ConfigurationEvent event) {
            boolean beforeUpdate = event.isBeforeUpdate();
            if (propagateEventToParent) {
                int type = event.getType();
                String name = event.getPropertyName();
                Object value = event.getPropertyValue();
                Object finalValue = null;
                switch(type) {
                case HierarchicalConfiguration.EVENT_ADD_NODES:
                case EVENT_CLEAR:
                case EVENT_CONFIGURATION_SOURCE_CHANGED:
                    fireEvent(type, name, value, beforeUpdate);
                    break;

                case EVENT_ADD_PROPERTY:
                case EVENT_SET_PROPERTY:
                    if (beforeUpdate) {
                        // we want the validators to run even if the source is not
                        // the winning configuration
                        fireEvent(type, name, value, beforeUpdate);                                                
                    } else {
                        AbstractConfiguration sourceConfig = (AbstractConfiguration) event.getSource();
                        AbstractConfiguration winningConf = (AbstractConfiguration) getSource(name);
                        if (winningConf == null || getIndexOfConfiguration(sourceConfig) <= getIndexOfConfiguration(winningConf)) {
                            fireEvent(type, name, value, beforeUpdate);                        
                        } 
                    }
                    break;
                case EVENT_CLEAR_PROPERTY:
                    finalValue = ConcurrentCompositeConfiguration.this.getProperty(name);
                    if (finalValue == null) {
                        fireEvent(type, name, value, beforeUpdate);                        
                    } else {
                        fireEvent(EVENT_SET_PROPERTY, name, finalValue, beforeUpdate);
                    }
                    break;
                default:
                    break;

                }
            }            
        }        
    };
    
    /**
     * Creates an empty CompositeConfiguration object which can then
     * be added some other Configuration files
     */
    public ConcurrentCompositeConfiguration()
    {
        clear();
    }

    
    /**
     * Creates a ConcurrentCompositeConfiguration object with a specified <em>container
     * configuration</em>. This configuration will store any changes made by {@link #setProperty(String, Object)}
     * and {@link #addProperty(String, Object)}. 
     *
     * @param containerConfiguration the configuration to use as container configuration
     */
    public ConcurrentCompositeConfiguration(AbstractConfiguration containerConfiguration)
    {
        configList.clear();
        this.containerConfiguration = containerConfiguration;
        configList.add(containerConfiguration);
    }


    /**
     * Creates a ConcurrentCompositeConfiguration with a specified <em>container
     * configuration</em>, and then adds the given collection of configurations.
     *
     * @param containerConfiguration container configuration to use
     * @param configurations        the collection of configurations to add
     */
    public ConcurrentCompositeConfiguration(AbstractConfiguration containerConfiguration,
            Collection<? extends AbstractConfiguration> configurations)
    {
        this(containerConfiguration);

        if (configurations != null)
        {
            for (AbstractConfiguration c : configurations)
            {
                addConfiguration(c);
            }
        }
    }

    /**
     * Event listener call back for configuration update events. This method is
     * called whenever one of the contained configurations was modified. This method 
     * does nothing.
     *
     * @param event the update event
     */
    @Override
    public void configurationChanged(ConfigurationEvent event)
    {
    }

    public void invalidate()
    {
    }

    /**
     * Add a child configuration without a name. Make a call to {@link #addConfiguration(AbstractConfiguration, String)}
     * with the name being null.
     *
     * @param config the configuration to add
     */
    public final void addConfiguration(AbstractConfiguration config)
    {
        addConfiguration(config, null);
    }

    /**
     * Adds a new child configuration to this configuration with an optional
     * name. The configuration will be added to the end of the list
     * if <em>container configuration</em> has been changed to new one or no longer at the end of 
     * the list. Otherwise it will be added in front of the <em>container configuration</em>.
     *
     * @param config the configuration to add (must not be <b>null</b>)
     * @param name the name of this configuration (can be <b>null</b>)
     */
    public void addConfiguration(AbstractConfiguration config, String name)
    {
        if (containerConfigurationChanged) {
            addConfigurationAtIndex(config, name, configList.size());
        } else {
            addConfigurationAtIndex(config, name, configList.indexOf(containerConfiguration));
        }
    }


    /**
     * Get the configurations added.
     */
    public List<AbstractConfiguration> getConfigurations() {
        return Collections.unmodifiableList(configList);
    }
    
    public List<String> getConfigurationNameList()
    {
        List<String> list = new ArrayList<String>(configList.size());
        for (AbstractConfiguration configuration: configList)
        {
            boolean foundName = false;
            for (String name: namedConfigurations.keySet()) {
                if (configuration == namedConfigurations.get(name)) {
                    foundName = true;
                    list.add(name);
                    break;
                }
            }
            if (!foundName) {
                list.add(null);
            }
        }
        return list;
    }
    
    public int getIndexOfConfiguration(AbstractConfiguration config) {
        return configList.indexOf(config);
    }        
    
    public int getIndexOfContainerConfiguration() {
        return configList.indexOf(containerConfiguration);
    }
    
    private void checkIndex(int newIndex) {
        if (newIndex < 0 || newIndex > configList.size()) {
            throw new IndexOutOfBoundsException(newIndex + " is out of bounds of the size of configuration list " + configList.size());
        }
    }
        

    /**
     * Adds a child configuration and makes it the <em>container
     * configuration</em>. This means that all future property write operations
     * are executed on this configuration. Note that the current container 
     * configuration stays in the list of child configurations
     * at its current position, but it passes its role as container 
     * configuration to the new one.
     *
     * @param config the configuration to be added
     * @param name the name of the configuration to be added
     * @param index index to add this configuration
     * 
     * @throws IndexOutOfBoundsException
     */
    public void setContainerConfiguration(AbstractConfiguration config, String name, int index) throws IndexOutOfBoundsException {
        if (!configList.contains(config)) {
            checkIndex(index);
            containerConfigurationChanged = true;
            containerConfiguration = config;            
            addConfigurationAtIndex(config, name, index);
        } else {
            logger.warn(config + " is not added as it already exits");
        }        
    }
    
    /**
     * Change the position of the <em>container configuration</em> to a new index.
     * 
     * @throws IndexOutOfBoundsException
     */
    public void setContainerConfigurationIndex(int newIndex) throws IndexOutOfBoundsException {
        if (newIndex < 0 || newIndex >= configList.size()) {
            throw new IndexOutOfBoundsException("Cannot change to the new index " + newIndex + " in the list of size " + configList.size());
        } else if (newIndex == configList.indexOf(containerConfiguration)) {
            // nothing to do
            return;
        }
        
        containerConfigurationChanged = true;
        configList.remove(containerConfiguration);
        configList.add(newIndex, containerConfiguration);
    }
        
    /**
     * Add a configuration with a name at a particular index.
     * 
     * @throws IndexOutOfBoundsException 
     */
    public void addConfigurationAtIndex(AbstractConfiguration config, String name, int index) 
    throws IndexOutOfBoundsException {
        if (!configList.contains(config)) {
            checkIndex(index);
            configList.add(index, config);
            if (name != null) {
                namedConfigurations.put(name, config);
            }
            config.addConfigurationListener(eventPropagater);
            fireEvent(EVENT_CONFIGURATION_SOURCE_CHANGED, null, null, false);
        } else {
            logger.warn(config + " is not added as it already exits");
        }
    }
    
    
    public void addConfigurationAtFront(AbstractConfiguration config, String name) {
            addConfigurationAtIndex(config, name, 0);
    }
    
    /**
     * Remove a configuration. The container configuration cannot be removed.
     *
     * @param config The configuration to remove
     */
    public  boolean removeConfiguration(Configuration config)
    {
        // Make sure that you can't remove the inMemoryConfiguration from
        // the CompositeConfiguration object       
        if (!config.equals(containerConfiguration))
        {
            return configList.remove((AbstractConfiguration) config);
        }
        return false;
    }
    
    public AbstractConfiguration removeConfigurationAt(int index) {
        AbstractConfiguration config = configList.remove(index);
        String nameFound = null;
        for (String name: namedConfigurations.keySet()) {
            if (namedConfigurations.get(name) == config) {
                nameFound = name;
                break;
            }
        }
        if (nameFound != null) {
            namedConfigurations.remove(nameFound);
        }
        return config;
    }

    /**
     * Removes the configuration with the specified name.
     *
     * @param name the name of the configuration to be removed
     * @return the removed configuration (<b>null</b> if this configuration
     * was not found)
     */
    public Configuration removeConfiguration(String name)
    {
        Configuration conf = getConfiguration(name);
        if (conf != null)
        {
            removeConfiguration(conf);
        }
        return conf;
    }
    /**
     * Return the number of configurations.
     *
     * @return the number of configuration
     */
    public int getNumberOfConfigurations()
    {
        return configList.size();
    }

    /**
     * Removes all child configurations and reinitializes the <em>container 
     * configuration</em>. <strong>Attention:</strong> A new container
     * configuration is created; the old one is lost.
     */
    @Override
    public final void clear()
    {
        fireEvent(EVENT_CLEAR, null, null, true);
        configList.clear();
        namedConfigurations.clear();
        // recreate the in memory configuration
        containerConfiguration = new ConcurrentMapConfiguration();
        containerConfiguration.setThrowExceptionOnMissing(isThrowExceptionOnMissing());
        containerConfiguration.setListDelimiter(getListDelimiter());
        containerConfiguration.setDelimiterParsingDisabled(isDelimiterParsingDisabled());
        containerConfiguration.addConfigurationListener(eventPropagater);
        configList.add(containerConfiguration);
        
        overrideProperties = new ConcurrentMapConfiguration();
        overrideProperties.setThrowExceptionOnMissing(isThrowExceptionOnMissing());
        overrideProperties.setListDelimiter(getListDelimiter());
        overrideProperties.setDelimiterParsingDisabled(isDelimiterParsingDisabled());
        overrideProperties.addConfigurationListener(eventPropagater);
        
        fireEvent(EVENT_CLEAR, null, null, false);
        containerConfigurationChanged = false;
        invalidate();
    }

    /**
     * Override the same property in any other configurations in the list.
     */
    public void setOverrideProperty(String key, Object finalValue) {
        overrideProperties.setProperty(key, finalValue);
    }
    
    /**
     * Remove the overriding property set by {@link #setOverrideProperty(String, Object)}
     */
    public void clearOverrideProperty(String key) {
        overrideProperties.clearProperty(key);
    }
            
    /**
     * Set the property with the <em>container configuration</em>. 
     * <b>Warning: </b>{@link #getProperty(String)} on this key may not return the same value set by this method
     * if there is any other configuration that contain the same property and is in front of the 
     * <em>container configuration</em> in the configurations list.
     */
    @Override
    public void setProperty(String key, Object value) {
        containerConfiguration.setProperty(key, value);
    }

    /**
     * Add the property with the <em>container configuration</em>. 
     * <b>Warning: </b>{@link #getProperty(String)} on this key may not return the same value set by this method
     * if there is any other configuration that contain the same property and is in front of the 
     * <em>container configuration</em> in the configurations list.
     */
    @Override
    public void addProperty(String key, Object value) {
        containerConfiguration.addProperty(key, value);
    }
    
    /**
     * Clear the property with the <em>container configuration</em>. 
     * <b>Warning: </b>{@link #getProperty(String)} on this key may still return some value 
     * if there is any other configuration that contain the same property and is in front of the 
     * <em>container configuration</em> in the configurations list.
     */

    @Override
    public void clearProperty(String key) {
        containerConfiguration.clearProperty(key);
    }
    /**
     * Read property from underlying composite. It first checks if the property has been overridden
     * by {@link #setOverrideProperty(String, Object)} and if so return the overriding value.
     * Otherwise, it iterates through the list of sub configurations until it finds one that contains the
     * property and return the value from that sub configuration. It returns null of the property does
     * not exist.
     *
     * @param key key to use for mapping
     *
     * @return object associated with the given configuration key. null if it does not exist.
     */
    public Object getProperty(String key)
    {
        if (overrideProperties.containsKey(key)) {
            return overrideProperties.getProperty(key);
        }
        Configuration firstMatchingConfiguration = null;
        for (Configuration config : configList)
        {
            if (config.containsKey(key))
            {
                firstMatchingConfiguration = config;
                break;
            }
        }

        if (firstMatchingConfiguration != null)
        {
            return firstMatchingConfiguration.getProperty(key);
        }
        else
        {
            return null;
        }
    }

    /**
     * Get all the keys contained by sub configurations.
     * 
     * @throws ConcurrentModificationException if concurrent modification happens on any sub configuration
     * when it is iterated to get all the keys
     * 
     */
    public Iterator<String> getKeys() throws ConcurrentModificationException
    {
        Set<String> keys = new LinkedHashSet<String>();
        for (Iterator<String> it = overrideProperties.getKeys(); it.hasNext();) {
            keys.add(it.next());
        }
        for (Configuration config : configList)
        {
            for (Iterator<String> it = config.getKeys(); it.hasNext();)
            {
                try {
                    keys.add(it.next());
                } catch (ConcurrentModificationException e) {
                    logger.error("unexpected exception when iterating the keys for configuration " + config 
                            + " with name " + getNameForConfiguration(config));
                    throw e;
                }
            }
        }

        return keys.iterator();
    }

    
    private String getNameForConfiguration(Configuration config) {
        for (Map.Entry<String, AbstractConfiguration> entry: namedConfigurations.entrySet()) {
            if (entry.getValue() == config) {
                return entry.getKey();
            }
        }
        return null;
    }
    /**
     * Get the list of the keys contained in the sub configurations that match the
     * specified prefix.
     * 
     */
    @Override
    public Iterator<String> getKeys(String prefix)
    {
        Set<String> keys = new LinkedHashSet<String>();
        for (Iterator<String> it = overrideProperties.getKeys(prefix); it.hasNext();) {
            keys.add(it.next());
        }
        for (Configuration config : configList)
        {
            for (Iterator<String> it = config.getKeys(prefix); it.hasNext();)
            {
                keys.add(it.next());
            }
        }

        return keys.iterator();
    }

    /**
     * Returns a set with the names of all configurations contained in this
     * configuration. Of course here are only these configurations
     * listed, for which a name was specified when they were added.
     *
     * @return a set with the names of the contained configurations (never
     * <b>null</b>)
     */
    public Set<String> getConfigurationNames()
    {
        return namedConfigurations.keySet();
    }
    
    @Override
    public boolean isEmpty()
    {
        if (overrideProperties.isEmpty()) {
            return false;
        }
        for (Configuration config : configList)
        {
            if (!config.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if the any of the sub configurations contains the specified key.
     *
     * @param key the key whose presence in this configuration is to be tested
     *
     * @return <code>true</code> if the configuration contains a value for this
     *         key, <code>false</code> otherwise
     * 
     */
    @Override
    public boolean containsKey(String key)
    {
        if (overrideProperties.containsKey(key)) {
            return true;
        }
        for (Configuration config : configList)
        {
            if (config.containsKey(key))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a List of objects associated with the given configuration key.
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated List of value.
     * 
     */
    @Override
    public List getList(String key, List defaultValue)
    {
        List<Object> list = new ArrayList<Object>();

        // add all elements from the first configuration containing the requested key
        Iterator<AbstractConfiguration> it = configList.iterator();
        if (overrideProperties.containsKey(key)) {
            appendListProperty(list, overrideProperties, key);
        }
        while (it.hasNext() && list.isEmpty())
        {
            Configuration config = it.next();
            if ((config != containerConfiguration || containerConfigurationChanged) 
                    && config.containsKey(key))
            {
                appendListProperty(list, config, key);
            }
        }

        // add all elements from the in memory configuration
        if (list.isEmpty()) {
            appendListProperty(list, containerConfiguration, key);
        }

        if (list.isEmpty())
        {
            return defaultValue;
        }

        ListIterator<Object> lit = list.listIterator();
        while (lit.hasNext())
        {
            lit.set(interpolate(lit.next()));
        }

        return list;
    }

    /**
     * Get an array of strings associated with the given configuration key.
     * If the key doesn't map to an existing object an empty array is returned
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     *
     */
    @Override
    public String[] getStringArray(String key)
    {
        List<Object> list = getList(key);

        // transform property values into strings
        String[] tokens = new String[list.size()];

        for (int i = 0; i < tokens.length; i++)
        {
            tokens[i] = String.valueOf(list.get(i));
        }

        return tokens;
    }

    /**
     * Return the configuration at the specified index.
     *
     * @param index The index of the configuration to retrieve
     * @return the configuration at this index
     */
    public Configuration getConfiguration(int index)
    {
        return configList.get(index);
    }

    /**
     * Returns the configuration with the given name. This can be <b>null</b>
     * if no such configuration exists.
     *
     * @param name the name of the configuration
     * @return the configuration with this name
     */
    public Configuration getConfiguration(String name)
    {
        return namedConfigurations.get(name);
    }
    
    /**
     * Returns the <em>container configuration</em> In this configuration
     * changes are stored.
     *
     * @return the container configuration
     */
    public Configuration getContainerConfiguration()
    {
        return containerConfiguration;
    }

    /**
     * Returns a copy of this object. This implementation will create a deep
     * clone, i.e. all configurations contained in this composite will also be
     * cloned. This only works if all contained configurations support cloning;
     * otherwise a runtime exception will be thrown. Registered event handlers
     * won't get cloned.
     *
     */
    @Override
    public Object clone()
    {
        try
        {
            ConcurrentCompositeConfiguration copy = (ConcurrentCompositeConfiguration) super
                    .clone();
            copy.clearConfigurationListeners();
            copy.configList = new LinkedList<AbstractConfiguration>();
            copy.containerConfiguration = (AbstractConfiguration) ConfigurationUtils
                    .cloneConfiguration(getContainerConfiguration());
            copy.configList.add(copy.containerConfiguration);

            for (Configuration config : configList)
            {
                if (config != getContainerConfiguration())
                {
                    copy.addConfiguration((AbstractConfiguration) ConfigurationUtils
                            .cloneConfiguration(config));
                }
            }

            return copy;
        }
        catch (CloneNotSupportedException cnex)
        {
            // cannot happen
            throw new ConfigurationRuntimeException(cnex);
        }
    }

    /**
     * Sets a flag whether added values for string properties should be checked
     * for the list delimiter. This implementation ensures that the container 
     * configuration is correctly initialized.
     *
     * @param delimiterParsingDisabled the new value of the flag
     */
    @Override
    public void setDelimiterParsingDisabled(boolean delimiterParsingDisabled)
    {
        containerConfiguration.setDelimiterParsingDisabled(delimiterParsingDisabled);
        super.setDelimiterParsingDisabled(delimiterParsingDisabled);
    }

    /**
     * Sets the character that is used as list delimiter. This implementation
     * ensures that the container configuration is correctly initialized.
     *
     * @param listDelimiter the new list delimiter character
     */
    @Override
    public void setListDelimiter(char listDelimiter)
    {
        containerConfiguration.setListDelimiter(listDelimiter);
        super.setListDelimiter(listDelimiter);
    }

    /**
     * Returns the configuration source, in which the specified key is defined.
     * This method will iterate over all existing child configurations and check
     * whether they contain the specified key. The following constellations are
     * possible:
     * <ul>
     * <li>If the child configurations contains this key, the first one is returned.</li>
     * <li>If none of the child configurations contain the key, <b>null</b> is
     * returned.</li>
     * </ul>
     *
     * @param key the key to be checked
     * @return the source configuration of this key
     */
    public Configuration getSource(String key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("Key must not be null!");
        }

        if (overrideProperties.containsKey(key)) {
            return overrideProperties;
        }
        
        for (Configuration conf : configList)
        {
            if (conf.containsKey(key))
            {
                return conf;
            }
        }
        return null;
    }

    /**
     * Adds the value of a property to the given list. This method is used by
     * {@code getList()} for gathering property values from the child
     * configurations.
     *
     * @param dest the list for collecting the data
     * @param config the configuration to query
     * @param key the key of the property
     */
    private static void appendListProperty(List<Object> dest, Configuration config,
            String key)
    {
        Object value = config.getProperty(key);
        if (value != null)
        {
            if (value instanceof Collection)
            {
                Collection<?> col = (Collection<?>) value;
                dest.addAll(col);
            }
            else
            {
                dest.add(value);
            }
        }
    }


    /**
     * Return whether sub configurations should propagate events to
     * listeners to this configuration.
     */
    public final boolean isPropagateEventFromSubConfigurations() {
        return propagateEventToParent;
    }


    /**
     * Set whether sub configurations should propagate events to
     * listeners to this configuration. This is needed if this configuration
     * is used as the configuration source of {@link DynamicPropertyFactory}.
     * 
     * @param propagateEventToParent value to set
     */
    public final void setPropagateEventFromSubConfigurations(boolean propagateEventToParent) {
        this.propagateEventToParent = propagateEventToParent;
    }    
}
