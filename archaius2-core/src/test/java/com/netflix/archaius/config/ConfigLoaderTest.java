package com.netflix.archaius.config;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.Config;
import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.exceptions.ConfigException;

public class ConfigLoaderTest {
    
    @Test
    public void testLoadingOfNonExistantFile() throws ConfigException {
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .build();
        
        Config config = loader.newLoader().load("non-existant");
        Assert.assertTrue(config.isEmpty());
    }
}
