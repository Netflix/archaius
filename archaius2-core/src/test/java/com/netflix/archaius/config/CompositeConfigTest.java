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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class CompositeConfigTest {
    @Test
    public void basicTest() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");
        
        com.netflix.archaius.api.config.CompositeConfig libraries = new DefaultCompositeConfig();
        com.netflix.archaius.api.config.CompositeConfig application = new DefaultCompositeConfig();
        
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("lib", libraries)
                .withConfig("app", application)
                .withConfig("set", MapConfig.from(props))
                .build();

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
            .withStrLookup(config)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        application.replaceConfig("application", loader.newLoader().load("application"));
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        libraries.replaceConfig("libA", loader.newLoader().load("libA"));
        libraries.accept(new PrintStreamVisitor());
        
        config.accept(new PrintStreamVisitor());
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        libraries.replaceConfig("libB", loader.newLoader().load("libB"));
        
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
        
        com.netflix.archaius.api.config.CompositeConfig libraries = new DefaultCompositeConfig(true);
        com.netflix.archaius.api.config.CompositeConfig application = new DefaultCompositeConfig();
        
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("lib", libraries)
                .withConfig("app", application)
                .withConfig("set", MapConfig.from(props))
                .build();

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
            .withStrLookup(config)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        application.replaceConfig("application", loader.newLoader().load("application"));
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        libraries.replaceConfig("libA", loader.newLoader().load("libA"));
        libraries.accept(new PrintStreamVisitor());
        
        config.accept(new PrintStreamVisitor());
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        libraries.replaceConfig("libB", loader.newLoader().load("libB"));
        
        System.out.println(config.toString());
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertTrue(config.getBoolean("libB.loaded"));
        Assert.assertEquals("libB", config.getString("libA.overrideA"));
        
        config.accept(new PrintStreamVisitor());
    }
    
    @Test
    public void getKeysTest() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();
        composite.addConfig("a", EmptyConfig.INSTANCE);
        
        Iterator<String> iter = composite.getKeys();
        Assert.assertFalse(iter.hasNext());
        
        composite.addConfig("b", MapConfig.builder().put("b1", "A").put("b2",  "B").build());
        
        iter = composite.getKeys();
        Assert.assertEquals(set("b1", "b2"), set(iter));
        
        composite.addConfig("c", EmptyConfig.INSTANCE);
        
        iter = composite.getKeys();
        Assert.assertEquals(set("b1", "b2"), set(iter));
        
        composite.addConfig("d", MapConfig.builder().put("d1", "A").put("d2",  "B").build());
        composite.addConfig("e", MapConfig.builder().put("e1", "A").put("e2",  "B").build());
        
        iter = composite.getKeys();
        Assert.assertEquals(set("b1", "b2", "d1", "d2", "e1", "e2"), set(iter));
    }

    private static Set<String> set(String ... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }

    private static Set<String> set(Iterator<String> values) {
        Set<String> vals = new LinkedHashSet<>();
        values.forEachRemaining(e -> vals.add(e));
        return Collections.unmodifiableSet(vals);
    }
}
