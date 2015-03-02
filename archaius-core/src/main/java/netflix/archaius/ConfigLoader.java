package netflix.archaius;

import java.io.File;
import java.net.URL;

import netflix.archaius.exceptions.ConfigurationException;

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
public interface ConfigLoader {
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
    Config load(String name, String resourceName) throws ConfigurationException ;
    
    /**
     * Load a specific URL.  The URL is assumed to be fully formed.  The concrete ConfigLoader will
     * only need to check that the extension is supported (ex .properties)
     * 
     * @param name
     * @return
     */
    Config load(String name, URL url) throws ConfigurationException;
    
    /**
     * Load a specific file.  The URL is assumed to be fully formed.  The concrete ConfigLoader will
     * only need to check that the extension is supported (ex .properties)
     * 
     * @param file
     * @return
     */
    Config load(String name, File file) throws ConfigurationException;
    
    /**
     * Determine if the 
     * 
     * @param resourceName
     * @return
     */
    boolean canLoad(String resourceName);
    
    boolean canLoad(URL uri);
    
    boolean canLoad(File file);
}
