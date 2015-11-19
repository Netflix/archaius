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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * the same property in configuration that was added later.  It is however possible
 * to set a flag that reverses the override order.
 * 
 * @author elandau
 *
 * TODO: Optional cache of queried properties
 * TODO: Resolve method to collapse all the child configurations into a single config
 * TODO: Combine children and lookup into a single LinkedHashMap
 */
public class DefaultCompositeConfig extends AbstractConfig implements CompositeConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeConfig.class);
    
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
            CompositeConfig config = new DefaultCompositeConfig();
            for (Entry<String, Config> entry : configs.entrySet()) {
                config.addConfig(entry.getKey(), entry.getValue());
            }
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static CompositeConfig create() throws ConfigException {
        return DefaultCompositeConfig.builder().build();
    }
    
    private final CopyOnWriteArrayList<Config>  children  = new CopyOnWriteArrayList<Config>();
    private final Map<String, Config>           lookup    = new LinkedHashMap<String, Config>();
    private final ConfigListener                listener;
    private final boolean                       reversed;
    
    public DefaultCompositeConfig() {
        this(false);
    }
    
    public DefaultCompositeConfig(boolean reversed) {
        this.reversed = reversed;
        listener = new ConfigListener() {
            @Override
            public void onConfigAdded(Config config) {
                notifyConfigAdded(DefaultCompositeConfig.this);
            }

            @Override
            public void onConfigRemoved(Config config) {
                notifyConfigRemoved(DefaultCompositeConfig.this);
            }

            @Override
            public void onConfigUpdated(Config config) {
                notifyConfigUpdated(DefaultCompositeConfig.this);
            }

            @Override
            public void onError(Throwable error, Config config) {
                notifyError(error, DefaultCompositeConfig.this);
            }
        };
    }

    @Override
    public synchronized boolean addConfig(String name, Config child) throws ConfigException {
        return internalAddConfig(name, child);
    }
    
    private synchronized boolean internalAddConfig(String name, Config child) throws ConfigException {
        LOG.trace("Adding config {} to {}", name, hashCode());
        
        if (child == null) {
            // TODO: Log a warning?
            return false;
        }
        
        if (name == null) {
            throw new ConfigException("Child configuration must be named");
        }
        
        if (lookup.containsKey(name)) {
            LOG.info("Configuration with name'{}' already exists", name);
            return false;
        }

        lookup.put(name, child);
        if (reversed) {
            children.add(0, child);
        }
        else {
            children.add(child);
        }
        child.addListener(listener);
        postConfigAdded(child);
        return true;
    }

    @Override
    public synchronized void addConfigs(LinkedHashMap<String, Config> configs) throws ConfigException {
        for (Entry<String, Config> entry : configs.entrySet()) {
            internalAddConfig(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void replaceConfigs(LinkedHashMap<String, Config> configs) throws ConfigException {
        for (Entry<String, Config> entry : configs.entrySet()) {
            replaceConfig(entry.getKey(), entry.getValue());
        }
    }

    @Override
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

    @Override
    public synchronized void replaceConfig(String name, Config child) throws ConfigException {
        internalRemoveConfig(name);
        internalAddConfig(name, child);
    }

    @Override
    public synchronized Config removeConfig(String name) {
        return internalRemoveConfig(name);
    }
    
    public synchronized Config internalRemoveConfig(String name) {
        Config child = this.lookup.remove(name);
        if (child != null) {
            this.children.remove(child);
            child.removeListener(listener);
            this.notifyConfigRemoved(child);
            return child;
        }
        return null;
    }    

    @Override
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
    public <T> List<T> getList(String key, Class<T> type) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child.getList(key, type);
            }
        }
        return notFound(key);
    }

    @Override
    public List getList(String key) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child.getList(key);
            }
        }
        return notFound(key);
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

    /**
     * Return a set of all unique keys tracked by any child of this composite.
     * This can be an expensive operations as it requires iterating through all of
     * the children.
     * 
     * TODO: Cache keys
     */
    @Override
    public Iterator<String> getKeys() {
        HashSet<String> result = new HashSet<>();
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
    public synchronized <T> T accept(Visitor<T> visitor) {
        T result = null;
        if (visitor instanceof CompositeVisitor) {
            synchronized (this) {
                CompositeVisitor<T> cv = (CompositeVisitor<T>)visitor;
                for (Entry<String, Config> entry : lookup.entrySet()) {
                    result = cv.visitChild(entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            for (Config child : children) {
                result = child.accept(visitor);
            }
        }
        return result;
    }

    public static CompositeConfig from(LinkedHashMap<String, Config> load) throws ConfigException {
        Builder builder = builder();
        for (Entry<String, Config> config : load.entrySet()) {
            builder.withConfig(config.getKey(), config.getValue());
        }
        return builder.build();
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
}
