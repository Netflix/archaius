package com.netflix.archaius.config;

import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.exceptions.ConfigException;

public class ConfigLoaderTest {
    
    @Test(expected=ConfigException.class)
    public void shouldFailWithNoApplicationConfig() throws ConfigException {
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .build();
        
        loader.newLoader().load("non-existant");
    }
    
    @Test
    public void shouldNotFailWithNoApplicationConfig() throws ConfigException {
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withFailOnFirst(false)
                .build();
        
        loader.newLoader().load("non-existant");
    }

}
