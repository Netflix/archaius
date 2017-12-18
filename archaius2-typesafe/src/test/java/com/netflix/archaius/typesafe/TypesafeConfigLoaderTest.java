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
package com.netflix.archaius.typesafe;

import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.config.DefaultCompositeConfig;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.api.exceptions.ConfigException;

public class TypesafeConfigLoaderTest {
    @Test
    public void test() throws ConfigException {
        CompositeConfig config = new DefaultCompositeConfig();
        config.addConfig("prop", MapConfig.builder()
                .put("env",    "prod")
                .put("region", "us-east")
                .build());

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withConfigReader(new TypesafeConfigReader())
                .withStrLookup(config)
                .build();

        config.replaceConfig("foo", loader.newLoader()
              .withCascadeStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
              .load("foo"));

        Assert.assertEquals("prod", config.getString("@environment"));
        Assert.assertEquals("foo-prod", config.getString("foo.prop1"));
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }

    @Test
    public void testWithLists() throws ConfigException {
        CompositeConfig config = new DefaultCompositeConfig();

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withConfigReader(new TypesafeConfigReader())
                .withStrLookup(config)
                .build();

        config.addConfig("config-with-list", loader.newLoader()
                .load("config-with-list"));

        ConfigProxyFactory proxyFactory = new ConfigProxyFactory(config);

        TestApplicationConfig testApplicationConfig = proxyFactory.newProxy(TestApplicationConfig.class);

        Assert.assertEquals(2, testApplicationConfig.getModuleWithSomePlugins().size());
        Assert.assertEquals(0, testApplicationConfig.getModuleWithNoPlugins().size());
        Assert.assertEquals(0, testApplicationConfig.getModuleWithNonExistingPlugins().size());
        Assert.assertEquals("plugin1", testApplicationConfig.getModuleWithSomePlugins().get(0));
        Assert.assertEquals("plugin2", testApplicationConfig.getModuleWithSomePlugins().get(1));

        Assert.assertEquals(1, testApplicationConfig.getModuleWithSomePluginsMatrix().get("plugin1").size());
        Assert.assertEquals(2, testApplicationConfig.getModuleWithSomePluginsMatrix().get("plugin2").size());

        Assert.assertEquals("Hello", testApplicationConfig.getModuleWithSubConfig().getVar1());
        Assert.assertEquals(true, testApplicationConfig.getModuleWithSubConfig().getVar2());
    }

}
