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
package com.netflix.archaius.readers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigReader;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertiesConfigReader implements ConfigReader {
    private String              includeKey      = "@next";

    @Override
    public Config load(ClassLoader loader, String resourceName, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        URL url = getResource(loader, resourceName + ".properties");
        if (url == null) {
            throw new ConfigException("Unable to resolve URL for resource " + resourceName);
        }
        return load(loader, url, strInterpolator, lookup);
    }

    @Override
    public Config load(ClassLoader loader, URL url, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        Properties props = new Properties();
        
        URL urlToLoad = url;
        LinkedList<String> nextUrls = new LinkedList<>();
        boolean loaded = false;
        do {
            try {
                Map<String, String> p = new URLConfigReader(urlToLoad).call().getToAdd();
                loaded = true;
                for (Entry<String, String> entry : p.entrySet()) {
                    props.put(entry.getKey(), entry.getValue());
                }

                String next = p.get(includeKey);
                if (next != null) {
                    nextUrls.addAll(new LinkedList<>(Arrays.asList(next.split(","))));
                }
            } catch (IOException e) {
                if ( !loaded && nextUrls.isEmpty() ) {
                    throw new ConfigException("Unable to load configuration " + url, e);
                }
            } finally {
                urlToLoad = nextUrls.isEmpty()
                    ? null
                    : getResource(loader, strInterpolator.create(lookup).resolve(nextUrls.remove()));
            }
        } while (urlToLoad != null);

        return new MapConfig(props);
        }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return getResource(loader, name + ".properties") != null;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return uri.getPath().endsWith(".properties");
    }

    private static URL getResource(ClassLoader loader, String resourceName)
    {
        URL url = null;
        // attempt to load from the context classpath
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        
        if (loader != null) {
            url = loader.getResource(resourceName);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = ClassLoader.getSystemResource(resourceName);
        }
        if (url == null) {
            try {
                resourceName = URLDecoder.decode(resourceName, "UTF-8");
                url = (new File(resourceName)).toURI().toURL();
            } catch (Exception e) {

            }
        }
        return url;
    }
}
