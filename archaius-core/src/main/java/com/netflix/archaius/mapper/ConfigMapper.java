package com.netflix.archaius.mapper;

import com.netflix.archaius.Config;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.exceptions.MappingException;

/**
 * API for mapping configuration to an object based on annotations or naming convention
 * 
 * @author elandau
 *
 */
public interface ConfigMapper {

    /**
     * Map the configuration from the provided config object onto the injectee and use
     * the provided IoCContainer to inject named bindings.
     * 
     * @param injectee
     * @param config
     * @param ioc
     * @throws MappingException
     */
    <T> void mapConfig(T injectee, Config config, IoCContainer ioc) throws MappingException;
    
    /**
     * Map the configuration from the provided config object onto the injectee.
     * 
     * @param injectee
     * @param config
     * @param ioc
     * @throws MappingException
     */
    <T> void mapConfig(T injectee, Config config) throws MappingException;

    /**
     * Create a proxy for the provided interface type for which all getter methods are bound
     * to a Property.
     * 
     * @param type
     * @param config
     * @return
     */
    <T> T newProxy(Class<T> type, PropertyFactory factory);
}
