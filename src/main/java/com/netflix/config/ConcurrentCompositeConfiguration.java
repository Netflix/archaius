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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.event.ConfigurationErrorListener;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationErrorEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConcurrentCompositeConfiguration maintains a list of sub configurations. Configurations with higher priority should be added 
 * before those with lower priority. 
 * For example, if configuration contains runtime overrides is added before configuration that 
 * contains system properties, the final value of property
 * will be taken from the runtime if both configurations contain the same key. 
 * <p>
 * The implementation of this class is based on Apache's CompositeConfiguration. It
 * allows you to add multiple {@code Configuration}
 * objects to an aggregated configuration. If you add Configuration1, and then Configuration2,
 * any properties shared will mean that the value defined by Configuration1
 * will be returned. If Configuration1 doesn't have the property, then
 * Configuration2 will be checked. You can add multiple different types or the
 * same type of properties file.</p>
 * <p>When querying properties the order in which child configurations have been
 * added is relevant. To deal with property updates, a so-called <em>in-memory
 * configuration</em> is used. Per default, such a configuration is created
 * automatically. All property writes target this special configuration. There
 * are constructors which allow you to provide a specific in-memory configuration.
 * If used that way, the in-memory configuration is always the last one in the
 * list of child configurations. This means that for query operations all other
 * configurations take precedence.</p>
 * <p>Alternatively it is possible to mark a child configuration as in-memory
 * configuration when it is added. In this case the treatment of the in-memory
 * configuration is slightly different: it remains in the list of child
 * configurations at the position it was added, i.e. its priority for property
 * queries can be defined by adding the child configurations in the correct
 * order.</p>
 * 
 * This class adds with the following changes/improvements to ComositeConfiguration:
 * <ul>
 * <li>It holds the list of sub configuration on a CopyOnWriteArrayList, which is thread safe and does not throw 
 * ConcurrentModificationException when it is modified while traversing its iterator.
 * <li>Its clearPropertyDirect() does not remove any property in the list of sub configurations other than 
 * the one designated as in memory configuration.
 * <li>It maintains an additional Map that maps sub configuration to a name.
 * </ul>
 * 
 * When adding configuration to this class, it is recommended to convert it into
 * {@link ConcurrentMapConfiguration} or ConcurrentCompositeConfiguration using 
 * {@link com.netflix.config.util.ConfigurationUtils} to achieve
 * maximal performance and thread safety.
 * 
 * @author awang
 *
 */
public class ConcurrentCompositeConfiguration extends AbstractConfiguration 
        implements ConfigurationListener, Cloneable {

    private Map<String, AbstractConfiguration> namedConfigurations = new ConcurrentHashMap<String, AbstractConfiguration>();
    
    private List<AbstractConfiguration> configList = new CopyOnWriteArrayList<AbstractConfiguration>();
    
    private Collection<ConfigurationListener> listeners = new CopyOnWriteArrayList<ConfigurationListener>();
    
    private Collection<ConfigurationErrorListener> errorListeners = new CopyOnWriteArrayList<ConfigurationErrorListener>();
    
    private AtomicLong detailEventsCount = new AtomicLong();
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentCompositeConfiguration.class);
    
    public static final int EVENT_CONFIGURATION_SOURCE_CHANGED = 10001;
    
    private volatile boolean propagateEventToParent = true;
    
    /**
     * Configuration that holds in memory stuff.  Inserted as first so any
     * setProperty() override anything else added.
     */
    private AbstractConfiguration inMemoryConfiguration;

    /**
     * Stores a flag whether the current in-memory configuration is also a
     * child configuration.
     */
    private volatile boolean inMemoryConfigIsChild = true;

    private ConfigurationListener eventPropagater = new ConfigurationListener() {
        @Override
        public void configurationChanged(ConfigurationEvent event) {
            if (!event.isBeforeUpdate() && propagateEventToParent) {
                int type = event.getType();
                String name = event.getPropertyName();
                Object value = event.getPropertyValue();
                Object finalValue = null;
                switch(type) {
                case HierarchicalConfiguration.EVENT_ADD_NODES:
                case AbstractConfiguration.EVENT_CLEAR:
                case EVENT_CONFIGURATION_SOURCE_CHANGED:
                    fireEvent(type, name, value, false);
                    break;

                case AbstractConfiguration.EVENT_ADD_PROPERTY:
                case AbstractConfiguration.EVENT_SET_PROPERTY:
                    finalValue = ConcurrentCompositeConfiguration.this.getProperty(name);
                    if (finalValue == null && value == null) {
                        fireEvent(type, name, null, false);                        
                    } else if (finalValue != null && finalValue.equals(value)) {
                        fireEvent(type, name, value, false);
                    }
                    break;
                case AbstractConfiguration.EVENT_CLEAR_PROPERTY:
                    finalValue = ConcurrentCompositeConfiguration.this.getProperty(name);
                    if (finalValue == null) {
                        fireEvent(type, name, value, false);                        
                    } else {
                        fireEvent(AbstractConfiguration.EVENT_SET_PROPERTY, name, finalValue, false);
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
     * Creates a CompositeConfiguration object with a specified <em>in-memory
     * configuration</em>. This configuration will store any changes made to the
     * {@code CompositeConfiguration}. Note: Use this constructor if you want to
     * set a special type of in-memory configuration. If you have a
     * configuration which should act as both a child configuration and as
     * in-memory configuration, use
     * {@link #addConfiguration(Configuration, boolean)} with a value of
     * <b>true</b> instead.
     *
     * @param inMemoryConfiguration the in memory configuration to use
     */
    public ConcurrentCompositeConfiguration(AbstractConfiguration inMemoryConfiguration)
    {
        configList.clear();
        this.inMemoryConfiguration = inMemoryConfiguration;
        configList.add(inMemoryConfiguration);
    }


    /**
     * Creates a CompositeConfiguration with a specified <em>in-memory
     * configuration</em>, and then adds the given collection of configurations.
     *
     * @param inMemoryConfiguration the in memory configuration to use
     * @param configurations        the collection of configurations to add
     * @see #CompositeConfiguration(Configuration)
     */
    public ConcurrentCompositeConfiguration(AbstractConfiguration inMemoryConfiguration,
            Collection<? extends AbstractConfiguration> configurations)
    {
        this(inMemoryConfiguration);

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
     * Add a configuration.
     *
     * @param config the configuration to add
     */
    public final void addConfiguration(AbstractConfiguration config)
    {
        addConfiguration(config, null, false);
    }

    /**
     * Adds a new configuration to this combined configuration with an optional
     * name. 
     *
     * @param config the configuration to add (must not be <b>null</b>)
     * @param name the name of this configuration (can be <b>null</b>)
     */
    public void addConfiguration(AbstractConfiguration config, String name)
    {
        addConfiguration(config, name,  false);
    }


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
    
    public int getIndexOfInMemoryConfiguration() {
        return configList.indexOf(inMemoryConfiguration);
    }
    
    /**
     * Adds a child configuration and optionally makes it the <em>in-memory
     * configuration</em>. This means that all future property write operations
     * are executed on this configuration. Note that the current in-memory
     * configuration is replaced by the new one. If it was created automatically
     * or passed to the constructor, it is removed from the list of child
     * configurations! Otherwise, it stays in the list of child configurations
     * at its current position, but it passes its role as in-memory
     * configuration to the new one.
     *
     * @param config the configuration to be added
     * @param name the name of the configuration to be added
     * @param asInMemory <b>true</b> if this configuration becomes the new
     *        <em>in-memory</em> configuration, <b>false</b> otherwise
     * @since 1.8
     */
    public void addConfiguration(AbstractConfiguration config, String name, boolean asInMemory)
    {
        if (!configList.contains(config))
        {
            if (asInMemory)
            {
                replaceInMemoryConfiguration(config);
                inMemoryConfigIsChild = true;
            }

            if (!inMemoryConfigIsChild)
            {
                // As the inMemoryConfiguration contains all manually added
                // keys, we must make sure that it is always last. "Normal", non
                // composed configurations add their keys at the end of the
                // configuration and we want to mimic this behavior.
                // configList.add(configList.indexOf(inMemoryConfiguration),
                //        config);
                addConfigurationAtIndex(config, name, configList.indexOf(inMemoryConfiguration));
            }
            else
            {
                // However, if the in-memory configuration is a regular child,
                // only the order in which child configurations are added is
                // relevant
                // configList.add(config);
                addConfigurationAtIndex(config, name, -1);
            }
        } else {
            logger.warn(config + " is not added as it already exits");
        }
    }
    
    public void addConfigurationAtIndex(AbstractConfiguration config, String name, int index, boolean asInMemory) {
        if (!configList.contains(config)) {
            if (asInMemory) {
                replaceInMemoryConfiguration(config);
                inMemoryConfigIsChild = true;
            }
            addConfigurationAtIndex(config, name, index);
        } else {
            logger.warn(config + " is not added as it already exits");
        }
    }
    
    public void addConfigurationAtIndex(AbstractConfiguration config, String name, int index) {
        if (!configList.contains(config)) {
            if (index < 0) {
                configList.add(config);
            } else {
                configList.add(index, config);
            }
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
     * Remove a configuration. The in memory configuration cannot be removed.
     *
     * @param config The configuration to remove
     */
    public  boolean removeConfiguration(Configuration config)
    {
        // Make sure that you can't remove the inMemoryConfiguration from
        // the CompositeConfiguration object       
        if (!config.equals(inMemoryConfiguration))
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
     * Removes all child configurations and reinitializes the <em>in-memory
     * configuration</em>. <strong>Attention:</strong> A new in-memory
     * configuration is created; the old one is lost.
     */
    @Override
    public final void clear()
    {
        fireEvent(EVENT_CLEAR, null, null, true);
        configList.clear();
        namedConfigurations.clear();
        // recreate the in memory configuration
        inMemoryConfiguration = new ConcurrentMapConfiguration();
        inMemoryConfiguration.setThrowExceptionOnMissing(isThrowExceptionOnMissing());
        inMemoryConfiguration.setListDelimiter(getListDelimiter());
        inMemoryConfiguration.setDelimiterParsingDisabled(isDelimiterParsingDisabled());
        configList.add(inMemoryConfiguration);
        fireEvent(EVENT_CLEAR, null, null, false);
        inMemoryConfigIsChild = false;
        invalidate();
    }

    /**
     * Add this property to the inmemory Configuration.
     *
     * @param key The Key to add the property to.
     * @param token The Value to add.
     */
    @Override
    protected void addPropertyDirect(String key, Object token)
    {
        inMemoryConfiguration.addProperty(key, token);
    }

    /**
     * Read property from underlying composite
     *
     * @param key key to use for mapping
     *
     * @return object associated with the given configuration key.
     */
    public Object getProperty(String key)
    {
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

    public Iterator<String> getKeys()
    {
        Set<String> keys = new LinkedHashSet<String>();
        for (Configuration config : configList)
        {
            for (Iterator<String> it = config.getKeys(); it.hasNext();)
            {
                keys.add(it.next());
            }
        }

        return keys.iterator();
    }

    @Override
    public Iterator<String> getKeys(String key)
    {
        Set<String> keys = new LinkedHashSet<String>();
        for (Configuration config : configList)
        {
            for (Iterator<String> it = config.getKeys(key); it.hasNext();)
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
    
    public boolean isEmpty()
    {
        for (Configuration config : configList)
        {
            if (!config.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void clearPropertyDirect(String key)
    {
        inMemoryConfiguration.clearProperty(key);
    }

    public boolean containsKey(String key)
    {
        for (Configuration config : configList)
        {
            if (config.containsKey(key))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public List getList(String key, List defaultValue)
    {
        List<Object> list = new ArrayList<Object>();

        // add all elements from the first configuration containing the requested key
        Iterator<AbstractConfiguration> it = configList.iterator();
        while (it.hasNext() && list.isEmpty())
        {
            Configuration config = it.next();
            if ((config != inMemoryConfiguration || inMemoryConfigIsChild) 
                    && config.containsKey(key))
            {
                appendListProperty(list, config, key);
            }
        }

        // add all elements from the in memory configuration
        if (list.isEmpty()) {
            appendListProperty(list, inMemoryConfiguration, key);
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
     * Returns the &quot;in memory configuration&quot;. In this configuration
     * changes are stored.
     *
     * @return the in memory configuration
     */
    public Configuration getInMemoryConfiguration()
    {
        return inMemoryConfiguration;
    }

    /**
     * Returns a copy of this object. This implementation will create a deep
     * clone, i.e. all configurations contained in this composite will also be
     * cloned. This only works if all contained configurations support cloning;
     * otherwise a runtime exception will be thrown. Registered event handlers
     * won't get cloned.
     *
     * @return the copy
     * @since 1.3
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
            copy.inMemoryConfiguration = (AbstractConfiguration) ConfigurationUtils
                    .cloneConfiguration(getInMemoryConfiguration());
            copy.configList.add(copy.inMemoryConfiguration);

            for (Configuration config : configList)
            {
                if (config != getInMemoryConfiguration())
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
     * for the list delimiter. This implementation ensures that the in memory
     * configuration is correctly initialized.
     *
     * @param delimiterParsingDisabled the new value of the flag
     * @since 1.4
     */
    @Override
    public void setDelimiterParsingDisabled(boolean delimiterParsingDisabled)
    {
        inMemoryConfiguration.setDelimiterParsingDisabled(delimiterParsingDisabled);
        super.setDelimiterParsingDisabled(delimiterParsingDisabled);
    }

    /**
     * Sets the character that is used as list delimiter. This implementation
     * ensures that the in memory configuration is correctly initialized.
     *
     * @param listDelimiter the new list delimiter character
     * @since 1.4
     */
    @Override
    public void setListDelimiter(char listDelimiter)
    {
        inMemoryConfiguration.setListDelimiter(listDelimiter);
        super.setListDelimiter(listDelimiter);
    }

    /**
     * Returns the configuration source, in which the specified key is defined.
     * This method will iterate over all existing child configurations and check
     * whether they contain the specified key. The following constellations are
     * possible:
     * <ul>
     * <li>If exactly one child configuration contains the key, this
     * configuration is returned as the source configuration. This may be the
     * <em>in memory configuration</em> (this has to be explicitly checked by
     * the calling application).</li>
     * <li>If none of the child configurations contain the key, <b>null</b> is
     * returned.</li>
     * <li>If the key is contained in multiple child configurations or if the
     * key is <b>null</b>, a {@code IllegalArgumentException} is thrown.
     * In this case the source configuration cannot be determined.</li>
     * </ul>
     *
     * @param key the key to be checked
     * @return the source configuration of this key
     * @throws IllegalArgumentException if the source configuration cannot be
     * determined
     * @since 1.5
     */
    public Configuration getSource(String key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("Key must not be null!");
        }

        Configuration source = null;
        for (Configuration conf : configList)
        {
            if (conf.containsKey(key))
            {
                if (source != null)
                {
                    throw new IllegalArgumentException("The key " + key
                            + " is defined by multiple sources!");
                }
                source = conf;
            }
        }

        return source;
    }

    /**
     * Replaces the current in-memory configuration by the given one.
     *
     * @param config the new in-memory configuration
     */
    private void replaceInMemoryConfiguration(AbstractConfiguration config)
    {
        if (!inMemoryConfigIsChild)
        {
            // remove current in-memory configuration
            configList.remove(inMemoryConfiguration);
        }
        inMemoryConfiguration = config;
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
    public final boolean isPropagateEventToParent() {
        return propagateEventToParent;
    }


    /**
     * Set whether sub configurations should propagate events to
     * listeners to this configuration. This is needed if this configuration
     * is used as the configuration source of {@link DynamicPropertyFactory}.
     * 
     * @param propagateEventToParent value to set
     */
    public final void setPropagateEventToParent(boolean propagateEventToParent) {
        this.propagateEventToParent = propagateEventToParent;
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
