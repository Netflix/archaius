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

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigReader;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.DefaultCompositeConfig.Builder;
import com.netflix.archaius.config.MapConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertiesConfigReader implements ConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfigReader.class);
    
    private static final String[] INCLUDE_KEYS = { "@next", "netflixconfiguration.properties.nextLoad" };
    private static final String SUFFIX = ".properties";
    
    @Override
    public Config load(ClassLoader loader, String resourceName, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        Builder builder = DefaultCompositeConfig.builder();
        Collection<URL> resources = getResources(loader, resourceName);
        if (resources.size() > 1) {
            LOG.warn("Multiple resource files found for {}. {}." + 
                     "  All resources will be loaded with override order undefined.",
                     resourceName, resources);
        }
        
        for (URL url : resources) {
            builder.withConfig(url.toString(), load(loader, url, strInterpolator, lookup));
        }
        
        CompositeConfig config = builder.build();
        if (config.getConfigNames().isEmpty()) {
            throw new ConfigException("No resources found for '" + resourceName + SUFFIX + "'");
        }
        
        return config;
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
    
                // Recursively load any files referenced by one of several 'include' properties
                // in the file.  The property value contains a list of URL's to load, where the
                // last loaded file wins for any individual property collisions. 
                for (String nextLoadPropName : INCLUDE_KEYS) {
                    String nextLoadValue = (String)props.remove(nextLoadPropName);
                    if (nextLoadValue != null) {
                        for (String urlString : nextLoadValue.split(",")) {
                            for (URL nextUrl : getResources(loader, strInterpolator.create(lookup).resolve(urlString))) {
                                internalLoad(props, seenUrls, loader, nextUrl, strInterpolator, lookup);
                            }
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
        return getResources(loader, name) != null;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return uri.getPath().endsWith(SUFFIX);
    }

    private static Collection<URL> getResources(ClassLoader loader, String resourceName) {
        LinkedHashSet<URL> resources = new LinkedHashSet<URL>();
        if (!resourceName.endsWith(SUFFIX)) {
            resourceName += SUFFIX;
        }
        
        // attempt to load from the context classpath
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        
        if (loader != null) {
            try {
                resources.addAll(Collections.list(loader.getResources(resourceName)));
            } catch (IOException e) {
                LOG.debug("Failed to load resources for {}", resourceName, e);
            }
        }
        
        try {
            resources.addAll(Collections.list(ClassLoader.getSystemResources(resourceName)));
        } catch (IOException e) {
            LOG.debug("Failed to load resources for {}", resourceName, e);
        }
        
        try {
            resourceName = URLDecoder.decode(resourceName, "UTF-8");
            File file = new File(resourceName);
            if (file.exists()) {
                resources.add(file.toURI().toURL());
            }
        } catch (Exception e) {
            LOG.debug("Failed to load resources for {}", resourceName, e);
        }
        
        return resources;
    }
}
