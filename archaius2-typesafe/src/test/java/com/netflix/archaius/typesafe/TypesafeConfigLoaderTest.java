/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.typesafe;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.sun.org.apache.bcel.internal.util.ClassLoader;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;

public class TypesafeConfigLoaderTest {
    @Test
    public void test() throws ConfigException {
        CompositeConfig config = new DefaultCompositeConfig();
        config.addConfig("prop", MapConfig.builder()
                .put("env", "prod")
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
    public void testResourceLoad() throws ConfigException {
        Config config = new TypesafeConfigReader().load("foo.properties");
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }

    @Test
    public void testFileLoad() throws ConfigException {
        try {
            File file = new File(ClassLoader.getSystemResource("foo.properties").toURI());
            Config config = new TypesafeConfigReader().load(file);
            Assert.assertEquals("foo", config.getString("foo.prop2"));
        } catch (URISyntaxException e) {
            throw new ConfigException("", e);
        }
    }

    @Test
    public void testReaderLoad() throws ConfigException {
        Reader reader = new InputStreamReader(ClassLoader.getSystemResourceAsStream("foo.properties"));
        Config config = new TypesafeConfigReader().load(reader);
        Assert.assertEquals("foo", config.getString("foo.prop2"));
    }
}
