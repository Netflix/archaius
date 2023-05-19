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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.instrumentation.AccessMonitorUtil;
import com.netflix.archaius.api.instrumentation.PropertyDetails;
import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

import static com.netflix.archaius.TestUtils.set;
import static com.netflix.archaius.TestUtils.size;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Test
    public void testGetKeysIteratorRemoveThrows() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();


        composite.addConfig("d", MapConfig.builder().put("d1", "A").put("d2",  "B").build());
        composite.addConfig("e", MapConfig.builder().put("e1", "A").put("e2",  "B").build());

        Iterator<String> keys = composite.getKeys();
        Assert.assertTrue(keys.hasNext());
        keys.next();
        Assert.assertThrows(UnsupportedOperationException.class, keys::remove);
    }

    @Test
    public void testKeysIterable() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();

        composite.addConfig("d", MapConfig.builder().put("d1", "A").put("d2",  "B").build());
        composite.addConfig("e", MapConfig.builder().put("e1", "A").put("e2",  "B").build());

        Iterable<String> keys = composite.keys();

        Assert.assertEquals(4, size(keys));
        Assert.assertEquals(set("d1", "d2", "e1", "e2"), set(keys));
    }

    @Test
    public void testKeysIterableModificationThrows() throws ConfigException {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();

        composite.addConfig("d", MapConfig.builder().put("d1", "A").put("d2",  "B").build());
        composite.addConfig("e", MapConfig.builder().put("e1", "A").put("e2",  "B").build());

        Assert.assertThrows(UnsupportedOperationException.class, composite.keys().iterator()::remove);
        Assert.assertThrows(UnsupportedOperationException.class, ((Collection<String>) composite.keys())::clear);
    }

    @Test
    public void unusedCompositeConfigIsGarbageCollected() throws ConfigException {
        SettableConfig sourceConfig = new DefaultSettableConfig();
        com.netflix.archaius.api.config.CompositeConfig config = DefaultCompositeConfig.builder()
                .withConfig("settable", sourceConfig)
                .build();
        Reference<Config> weakReference = new WeakReference<>(config);

        // No more pointers to prefix means this should be garbage collected and any additional listeners on it
        config = null;
        System.gc();
        Assert.assertNull(weakReference.get());
    }

    @Test
    public void instrumentationNotEnabled() throws Exception {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();

        composite.addConfig("polling", createPollingDynamicConfig("a1", "1", "b1", "2"));

        Assert.assertFalse(composite.instrumentationEnabled());
        Assert.assertEquals(composite.getRawProperty("a1"), "1");
        Assert.assertEquals(composite.getRawProperty("b1"), "2");
    }

    @Test
    public void instrumentationPropagation() throws Exception {
        com.netflix.archaius.api.config.CompositeConfig composite = new DefaultCompositeConfig();
        AccessMonitorUtil accessMonitorUtil = spy(AccessMonitorUtil.builder().build());

        PollingDynamicConfig outerPollingDynamicConfig = createPollingDynamicConfig("a1", "1", "b1", "2");
        outerPollingDynamicConfig.setAccessMonitorUtil(accessMonitorUtil);
        composite.addConfig("outer", outerPollingDynamicConfig);

        com.netflix.archaius.api.config.CompositeConfig innerComposite = new DefaultCompositeConfig();
        PollingDynamicConfig nestedPollingDynamicConfig = createPollingDynamicConfig("b1", "1", "c1", "3");
        nestedPollingDynamicConfig.setAccessMonitorUtil(accessMonitorUtil);
        innerComposite.addConfig("polling", nestedPollingDynamicConfig);
        composite.addConfig("innerComposite", innerComposite);

        composite.addConfig("d", MapConfig.builder().put("c1", "4").put("d1",  "5").build());

        // Properties (a1: 1) and (b1: 2) are covered by the first polling config
        Assert.assertEquals(composite.getRawProperty("a1"), "1");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("a1", "a1", "1")));

        Assert.assertEquals(composite.getRawPropertyUninstrumented("a1"), "1");
        verify(accessMonitorUtil, times(1)).registerUsage(any());

        Assert.assertEquals(composite.getRawProperty("b1"), "2");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("b1", "b1", "2")));

        Assert.assertEquals(composite.getRawPropertyUninstrumented("b1"), "2");
        verify(accessMonitorUtil, times(2)).registerUsage(any());

        // Property (c1: 3) is covered by the composite config over the polling config
        Assert.assertEquals(composite.getRawProperty("c1"), "3");
        verify(accessMonitorUtil).registerUsage(eq(new PropertyDetails("c1", "c1", "3")));

        Assert.assertEquals(composite.getRawPropertyUninstrumented("c1"), "3");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        // Property (d1: 5) is covered by the final, uninstrumented MapConfig
        Assert.assertEquals(composite.getRawProperty("d1"), "5");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        Assert.assertEquals(composite.getRawPropertyUninstrumented("d1"), "5");
        verify(accessMonitorUtil, times(3)).registerUsage(any());

        // The instrumented forEachProperty endpoint updates the counts for every property
        composite.forEachProperty((k, v) -> {});
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("a1", "a1", "1")));
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("b1", "b1", "2")));
        verify(accessMonitorUtil, times(2)).registerUsage(eq(new PropertyDetails("c1", "c1", "3")));
        verify(accessMonitorUtil, times(6)).registerUsage((any()));

        // The uninstrumented forEachProperty leaves the counts unchanged
        composite.forEachPropertyUninstrumented((k, v) -> {});
        verify(accessMonitorUtil, times(6)).registerUsage((any()));
    }

    private PollingDynamicConfig createPollingDynamicConfig(
            String key1, String value1, String key2, String value2) throws Exception {
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        Map<String, String> props = new HashMap<>();
        props.put(key1, value1);
        props.put(key2, value2);
        Map<String, String> propIds = new HashMap<>();
        propIds.put(key1, key1);
        propIds.put(key2, key2);
        Callable<PollingResponse> reader = () -> PollingResponse.forSnapshot(props, propIds);
        PollingDynamicConfig config = new PollingDynamicConfig(reader, strategy);
        strategy.fire();
        return config;
    }
}
