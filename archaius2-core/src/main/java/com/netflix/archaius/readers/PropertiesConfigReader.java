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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigReader;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;

public class PropertiesConfigReader implements ConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfigReader.class);
    
    private static final String INCLUDE_KEY = "@next";
    private static final String SUFFIX = ".properties";
    
    @Override
    public Config load(ClassLoader loader, String resourceName, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        URL url = getResource(loader, resourceName);
        if (url == null) {
            throw new ConfigException("Unable to resolve URL for resource " + resourceName);
        }
        return load(loader, url, strInterpolator, lookup);
    }

    @Override
    public Config load(ClassLoader loader, URL url, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        Properties props = new Properties();
        internalLoad(props, new HashSet<String>(), loader, url, strInterpolator, lookup);
        return MapConfig.from(props);
    }
    
    private void internalLoad(Properties props, Set<String> seenUrls, ClassLoader loader, URL url, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) {
        LOG.debug("Attempting to load : {}", url.toExternalForm());
        // Guard against circular dependencies 
        if (!seenUrls.contains(url.toExternalForm())) {
            seenUrls.add(url.toExternalForm());
            
            try {
                // Load properties into the single Properties object overriding any property
                // that may already exist
                Map<String, String> p = new URLConfigReader(url).call().getToAdd();
                LOG.debug("Loaded : {}", url.toExternalForm());
                props.putAll(p);
    
                // Recursively load any files referenced by an @next property in the file
                // Only one @next property is expected and the value may be a list of files
                String next = p.get(INCLUDE_KEY);
                if (next != null) {
                    p.remove(INCLUDE_KEY);
                    for (String urlString : next.split(",")) {
                        URL nextUrl = getResource(loader, strInterpolator.create(lookup).resolve(urlString));
                        if (nextUrl != null) {
                            internalLoad(props, seenUrls, loader, nextUrl, strInterpolator, lookup);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.debug("Unable to load configuration file {}. {}", url, e.getMessage());
            }
        }
        else {
            LOG.debug("Circular dependency trying to load url : {}", url.toExternalForm());
        }
    }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return getResource(loader, name) != null;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return uri.getPath().endsWith(SUFFIX);
    }

    private static URL getResource(ClassLoader loader, String resourceName) {
        if (!resourceName.endsWith(SUFFIX)) {
            resourceName += SUFFIX;
        }
        
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
