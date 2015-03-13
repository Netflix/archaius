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

import java.net.URL;

import com.netflix.archaius.ConfigReader;
import com.netflix.archaius.exceptions.ConfigException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.Missing;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class TypesafeConfigReader implements ConfigReader {
    @Override
    public com.netflix.archaius.Config load(ClassLoader loader, String name, String resourceName) throws ConfigException {
        Config config = ConfigFactory.parseResourcesAnySyntax(loader, resourceName);
        return new TypesafeConfig(name, config);
    }

    @Override
    public com.netflix.archaius.Config load(ClassLoader loader, String name, URL url) throws ConfigException {
        Config config = ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setClassLoader(loader));
        return new TypesafeConfig(name, config);
    }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return true;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return true;
    }
}
