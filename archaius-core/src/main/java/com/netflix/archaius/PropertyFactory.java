package com.netflix.archaius;


/**
 * SPI for a factory for binding to a Property for a property name.  
 * 
 * @see Property
 * @author elandau
 */
public interface PropertyFactory {
    /**
     * Create an observable for the property name.  
     */
    public PropertyContainer connectProperty(String propName);
}
