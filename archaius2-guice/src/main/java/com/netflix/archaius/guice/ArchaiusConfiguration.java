package com.netflix.archaius.guice;

import java.util.Map;
import java.util.Set;

import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.Decoder;

/**
 * Configuration interface for archaius
 * 
 * @author elandau
 *
 */
public interface ArchaiusConfiguration {
    /**
     * Return seeders for the runtime layer.  These seeders
     * are called after initial loading the system, env and application layer
     * 
     * @return Set of seeders or empty set if none specified
     */
    Set<ConfigSeeder> getRuntimeLayerSeeders();
    
    /**
     * Return seeders for the runtime layer.  These seeders
     * are called after initial loading the system, env and application layer
     * 
     * @return Set of seeders or empty set if none specified
     */
    Set<ConfigSeeder> getRemoteLayerSeeders();
    
    /**
     * Return seeders for the defaults layer.  
     * @return
     */
    Set<ConfigSeeder> getDefaultsLayerSeeders();

    /**
     * Return the application configuration name.  Default value is 'application'
     * 
     * @return Valid configuration name.  Must not be null or empty.
     */
    String getConfigName();
    
    /**
     * Return the default cascade strategy to use for library
     * and application configuration resource loading
     * 
     * @return Return a valid CascadeStrategy
     */
    CascadeStrategy getCascadeStrategy();

    /**
     * Return the main decoder to be used
     * 
     * @return Return a valid Decoder.
     */
    Decoder getDecoder();
    
    /**
     * Return a set of configuration listeners that will be registered before
     * any configuration is loaded
     * 
     * @return Set of listeners or empty set if non specified
     */
    Set<ConfigListener> getConfigListeners();
    
    /**
     * Return a map of library name to a Config override object.
     * These overrides take precedence over the main library configuration
     * as well as all the cascade override values
     * 
     * @return Map of overrides for libraries/resources.  Should return an empty map
     * if no overrides specified
     */
    Map<String, Config> getLibraryOverrides();
}
