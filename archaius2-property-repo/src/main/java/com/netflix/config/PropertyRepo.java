package com.netflix.config;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Properties accessor interface with neutral arguments and dependencies
 * 
 */
public interface PropertyRepo {

    // simple property suppliers with static default values
    
    Supplier<Boolean> getProperty(String propertyKey, Boolean defaultValue);

    Supplier<Integer> getProperty(String propertyKey, Integer defaultValue);

    Supplier<Long> getProperty(String propertyKey, Long defaultValue);

    Supplier<String> getProperty(String propertyKey, String defaultValue);

    Supplier<Set<String>> getProperty(String propertyKey, Set<String> defaultValue);
    
    // chained properties with static default values

    Supplier<Boolean> getProperty(String overrideKey, String primaryKey, Boolean defaultValue);

    Supplier<Integer> getProperty(String overrideKey, String primaryKey, Integer defaultValue);
    
    Supplier<Long> getProperty(String overrideKey, String primaryKey, Long defaultValue);

    Supplier<String> getProperty(String overrideKey, String primaryKey, String defaultValue);

    Supplier<Set<String>> getProperty(String overrideKey, String propertyKey, Set<String> defaultValue);
    
    // supplier-defaulted properties

    Supplier<Boolean> getProperty(String overrideKey, Supplier<Boolean> primaryProperty, Boolean defaultValue);

    Supplier<Integer> getProperty(String overrideKey, Supplier<Integer> primaryProperty, Integer defaultValue);

    Supplier<Long> getProperty(String overrideKey, Supplier<Long> primaryProperty, Long defaultValue);

    Supplier<String> getProperty(String overrideKey, Supplier<String> primaryProperty, String defaultValue);

    Supplier<Set<String>> getProperty(String overrideKey, Supplier<Set<String>> primaryProperty, Set<String> defaultValue);
    
    // callback support for Suppliers returned by this interface

    <T> Supplier<T> onChange(Supplier<T> property, Runnable callback);

}