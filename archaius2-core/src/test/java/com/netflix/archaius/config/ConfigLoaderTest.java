package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.api.exceptions.ConfigException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigLoaderTest {
    
    @Test
    public void testLoadingOfNonExistantFile() throws ConfigException {
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .build();
        
        Config config = loader.newLoader().load("non-existant");
        assertTrue(config.isEmpty());
    }
}
