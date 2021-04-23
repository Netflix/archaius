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

import java.io.File;
import java.io.Reader;
import java.net.URL;

import com.netflix.archaius.api.ConfigReader;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class TypesafeConfigReader implements ConfigReader {
    @Override
    public com.netflix.archaius.api.Config load(ClassLoader loader, String resourceName, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        Config config = ConfigFactory.parseResourcesAnySyntax(loader, resourceName);
        return new TypesafeConfig(config);
    }

    public com.netflix.archaius.api.Config load(String resourceName) throws ConfigException {
        Config config = ConfigFactory.parseResourcesAnySyntax(resourceName);
        return new TypesafeConfig(config);
    }

    @Override
    public com.netflix.archaius.api.Config load(ClassLoader loader, URL url, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        Config config = ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setClassLoader(loader));
        return new TypesafeConfig(config);
    }

    public com.netflix.archaius.api.Config load(File file) throws ConfigException {
        Config config = ConfigFactory.parseFile(file);
        return new TypesafeConfig(config);
    }

    public com.netflix.archaius.api.Config load(Reader reader) throws ConfigException {
        Config config = ConfigFactory.parseReader(reader);
        return new TypesafeConfig(config);
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
