package com.netflix.archaius.api.config;

import java.util.Collection;
import java.util.LinkedHashMap;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.exceptions.ConfigException;

/**
 * Config that is a composite of multiple configuration and as such doesn't track
 * properties of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration based on some ordering.  Implementations of this
 * interface should specify and implement the override ordering.
 */
public interface CompositeConfig extends Config {

    static interface CompositeVisitor<T> extends Visitor<T> {
        /**
         * Visit a child of the configuration
         *
         * @param name
         * @param child
         * @return
         */
        T visitChild(String name, Config child);
    }
    
    /**
     * Add a named configuration.  The newly added configuration takes precedence over all
     * previously added configurations.  Duplicate configurations are not allowed
     * <p>
     * This will trigger an onConfigAdded event.
     *
     * @param name
     * @param child
     * @throws ConfigException
     */
    boolean addConfig(String name, Config child) throws ConfigException;
    
    /**
     * Replace the configuration with the specified name
     *
     * This will trigger an onConfigUpdated event.
     *
     * @param name
     * @param child
     * @throws ConfigException
     */
    void replaceConfig(String name, Config child) throws ConfigException;

    /**
     * Add a map of named configurations.  The newly added configurations takes precedence over all
     * previously added configurations.  Duplicate configurations are not allowed
     * <p>
     * This will trigger an onConfigAdded event.
     *
     * @param configs a map of [configName, config]
     * @throws ConfigException
     */
    void addConfigs(LinkedHashMap<String, Config> configs) throws ConfigException;

    /**
     * Replace all configurations with the specified names in the map
     *
     * This will trigger an onConfigUpdated event.
     *
     * @param configs a map of [configName, config]
     * @throws ConfigException
     */
    void replaceConfigs(LinkedHashMap<String, Config> configs) throws ConfigException;

    /**
     * Remove a named configuration.
     *
     * This will trigger an onConfigRemoved event.
     *
     * @param name
     * @return
     */
    Config removeConfig(String name);

    /**
     * Look up a configuration by name
     *
     * @param name the config name to look up
     * @return the Config that matches the name, null otherwise
     */
    Config getConfig(String name);

    /**
     *
     * @return a collection of all configNames
     */
    Collection<String> getConfigNames();

}
