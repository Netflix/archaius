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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.exceptions.ConfigException;

/**
 * Config that is a composite of multiple configuration and as such doesn't track 
 * properties of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration that was previously added.
 * 
 * @author elandau
 *
 * TODO: Optional cache of queried properties
 */
public class CompositeConfig extends AbstractConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeConfig.class);
    
    public static interface CompositeVisitor {
        void visit(String name, Config child);
    }
    
    /**
     * The builder provides a fluent style API to create a CompositeConfig
     * @author elandau
     */
    public static class Builder {
        LinkedHashMap<String, Config> configs = new LinkedHashMap<>();
        
        public Builder withConfig(String name, Config config) {
            configs.put(name, config);
            return this;
        }
        
        public CompositeConfig build() throws ConfigException {
            CompositeConfig config = new CompositeConfig();
            for (Entry<String, Config> entry : configs.entrySet()) {
                config.addConfig(entry.getKey(), entry.getValue());
            }
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private final CopyOnWriteArrayList<Config>  children  = new CopyOnWriteArrayList<Config>();
    private final Map<String, Config>           lookup    = new LinkedHashMap<String, Config>();
    private final ConfigListener                listener;
    
    public CompositeConfig() {
        listener = new ConfigListener() {
            @Override
            public void onConfigAdded(Config config) {
                notifyConfigAdded(CompositeConfig.this);
            }

            @Override
            public void onConfigRemoved(Config config) {
                notifyConfigRemoved(CompositeConfig.this);
            }

            @Override
            public void onConfigUpdated(Config config) {
                notifyConfigUpdated(CompositeConfig.this);
            }

            @Override
            public void onError(Throwable error, Config config) {
                notifyError(error, CompositeConfig.this);
            }
        };
    }
    
    /**
     * Add a named configuration.  The newly added configuration takes precedence over all
     * previously added configurations.  Duplicate configurations are not allowed
     * 
     * This will trigger an onConfigAdded event.
     * 
     * @param name
     * @param child
     * @throws ConfigException
     */
    public synchronized void addConfig(String name, Config child) throws ConfigException {
        LOG.trace("Adding config {} to {}", name, hashCode());
        
        if (name == null) {
            throw new ConfigException("Child configuration must be named");
        }
        
        if (child == null) {
            // TODO: Log a warning?
            return;
        }
        
        if (lookup.containsKey(name)) {
            throw new ConfigException("Configuration with name " + name + " already exists");
        }

        lookup.put(name, child);
        children.add(child);
        child.addListener(listener);
        postConfigAdded(child);
    }
    
    public synchronized Collection<String> getConfigNames() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.lookup.keySet());
        return result;
    }
    
    protected void postConfigAdded(Config child) {
        child.setStrInterpolator(getStrInterpolator());
        child.setDecoder(getDecoder());
        notifyConfigAdded(child);
        child.addListener(listener);
    }
    
    /**
     * Replace the configuration with the specified name
     *
     * This will trigger an onConfigUpdated event.
     * 
     * @param name
     * @param child
     * @throws ConfigException
     */
    public synchronized void replaceConfig(String name, Config child) throws ConfigException {
        if (lookup.containsKey(name)) {
            lookup.put(name, child);
            child.removeListener(listener);
            postConfigAdded(child);
        }
        else {
            addConfig(name, child);
        }
    }
    
    /**
     * Remove a named configuration.  
     * 
     * This will trigger an onConfigRemoved event.
     * 
     * @param name
     * @return
     */
    public synchronized boolean removeConfig(String name) {
        Config child = this.lookup.remove(name);
        if (child != null) {
            this.children.remove(child);
            child.removeListener(listener);
            this.notifyConfigRemoved(child);
            return true;
        }
        return false;
    }    
    
    /**
     * Look up a configuration by name
     * @param name
     * @return
     */
    public Config getConfig(String name) {
        return lookup.get(name);
    }

    @Override
    public Object getRawProperty(String key) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child.getRawProperty(key);
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (Config child : children) {
            if (!child.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public Iterator<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Config config : children) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                result.add(key);
            }
        }
        return result.iterator();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        for (Config child : children) {
            sb.append(child.toString()).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public synchronized void accept(Visitor visitor) {
        if (visitor instanceof CompositeVisitor) {
            synchronized (this) {
                CompositeVisitor cv = (CompositeVisitor)visitor;
                for (Entry<String, Config> entry : lookup.entrySet()) {
                    cv.visit(entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            for (Config child : children) {
                child.accept(visitor);
            }
        }
    }
}
