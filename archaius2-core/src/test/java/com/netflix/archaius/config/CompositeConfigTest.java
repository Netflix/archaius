/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.config;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class CompositeConfigTest {
    @Test
    public void basicTest() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");
        
        CompositeConfig libraries = new CompositeConfig();
        CompositeConfig application = new CompositeConfig();
        
        CompositeConfig config = CompositeConfig.builder()
                .withConfig("lib", libraries)
                .withConfig("app", application)
                .withConfig("set", MapConfig.from(props))
                .build();

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
            .withStrLookup(config)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        application.replaceConfigs(loader.newLoader().load("application"));
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        libraries.replaceConfigs(loader.newLoader().load("libA"));
        libraries.accept(new PrintStreamVisitor());
        
        config.accept(new PrintStreamVisitor());
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        libraries.replaceConfigs(loader.newLoader().load("libB"));
        
        System.out.println(config.toString());
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertTrue(config.getBoolean("libB.loaded"));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        config.accept(new PrintStreamVisitor());
    }
    
    @Test
    public void basicReversedTest() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");
        
        CompositeConfig libraries = new CompositeConfig(true);
        CompositeConfig application = new CompositeConfig();
        
        CompositeConfig config = CompositeConfig.builder()
                .withConfig("lib", libraries)
                .withConfig("app", application)
                .withConfig("set", MapConfig.from(props))
                .build();

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
            .withStrLookup(config)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        application.replaceConfigs(loader.newLoader().load("application"));
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        libraries.replaceConfigs(loader.newLoader().load("libA"));
        libraries.accept(new PrintStreamVisitor());
        
        config.accept(new PrintStreamVisitor());
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        libraries.replaceConfigs(loader.newLoader().load("libB"));
        
        System.out.println(config.toString());
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertTrue(config.getBoolean("libB.loaded"));
        Assert.assertEquals("libB", config.getString("libA.overrideA"));
        
        config.accept(new PrintStreamVisitor());
    }
}
