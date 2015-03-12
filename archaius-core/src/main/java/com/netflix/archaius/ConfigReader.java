package com.netflix.archaius;

import java.net.URL;

import com.netflix.archaius.exceptions.ConfigException;

/**
 * Contract for a configuration file loader.  A ConfigManager will likely be configured with 
 * multiple configuration loaders, each responsible for loading a specific configuration
 * format and loading from a specific location.
 * 
 * TODO: Consider splitting load(resource) into a separate abstraction
 * 
 * @author elandau
 *
 */
public interface ConfigReader {
    /**
     * Load configuration from a simple resource name.  A concrete ConfigLoader will need to add
     * location and type information to this resource.
     * 
     * For example, an WebAppConfigurationLoader will attempt load to the configuration from
     *    resourceName : 'application-prod'
     *    
     *    /WEB-INF/confg/application-prod.properties
     *    
     * @param resourceName
     * @return
     */
    Config load(ClassLoader loader, String name, String resourceName) throws ConfigException ;
    
    /**
     * Load a specific URL.  The URL is assumed to be fully formed.  The concrete ConfigLoader will
     * only need to check that the extension is supported (ex .properties)
     * 
     * @param name
     * @return
     */
    Config load(ClassLoader loader, String name, URL url) throws ConfigException;
    
    /**
     * Determine if this reader can load the provided resource name
     * 
     * @param resourceName
     * @return
     */
    boolean canLoad(ClassLoader loader, String resourceName);

    /**
     * Determine if this reader can load the provided url
     * @param loader
     * @param uri
     * @return
     */
    boolean canLoad(ClassLoader loader, URL uril);
}
