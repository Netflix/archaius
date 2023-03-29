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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.exceptions.ConfigException;

/**
 * Config that is a composite of multiple configuration and as such doesn't track 
 * properties of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration that was added later.  It is however possible
 * to set a flag that reverses the override order.
 * 
 * TODO: Optional cache of queried properties
 * TODO: Resolve method to collapse all the child configurations into a single config
 * TODO: Combine children and lookup into a single LinkedHashMap
 */
public class DefaultCompositeConfig extends AbstractConfig implements com.netflix.archaius.api.config.CompositeConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCompositeConfig.class);
    
    /**
     * The builder provides a fluent style API to create a CompositeConfig
     */
    public static class Builder {
        LinkedHashMap<String, Config> configs = new LinkedHashMap<>();
        
        public Builder withConfig(String name, Config config) {
            configs.put(name, config);
            return this;
        }
        
        public com.netflix.archaius.api.config.CompositeConfig build() throws ConfigException {
            com.netflix.archaius.api.config.CompositeConfig config = new DefaultCompositeConfig();
            for (Entry<String, Config> entry : configs.entrySet()) {
                config.addConfig(entry.getKey(), entry.getValue());
            }
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static com.netflix.archaius.api.config.CompositeConfig create() throws ConfigException {
        return DefaultCompositeConfig.builder().build();
    }
    
    private class State {
        private final Map<String, Config> children;
        private final Map<String, Object> data;
        
        public State(Map<String, Config> children, int size) {
            this.children = children;
            Map<String, Object> data = new HashMap<>(size);
            children.values().forEach(child -> child.forEachProperty(data::putIfAbsent));
            this.data = Collections.unmodifiableMap(data);
        }
        
        State addConfig(String name, Config config) {
            LinkedHashMap<String, Config> children = new LinkedHashMap<>(this.children.size() + 1);
            if (reversed) {
                children.put(name, config);
                children.putAll(this.children);
            } else {
                children.putAll(this.children);
                children.put(name, config);
            }
            return new State(children, data.size() + 16);
        }
        
        State removeConfig(String name) {
            if (children.containsKey(name)) {
                LinkedHashMap<String, Config> children = new LinkedHashMap<>(this.children);
                children.remove(name);
                return new State(children, data.size());
            }
            return this;
        }
        
        public State refresh() {
            return new State(children, data.size());
        }


        Config getConfig(String name) {
            return children.get(name);
        }
        
        boolean containsConfig(String name) {
            return getConfig(name) != null;
        }
    }

    /**
     * Listener to be added to any component configs which updates the config map and triggers updates on all listeners
     * when any of the components are updated.
     */
    private static class CompositeConfigListener extends DependentConfigListener<DefaultCompositeConfig> {
        private CompositeConfigListener(DefaultCompositeConfig config) {
            super(config);
        }

        @Override
        public void onSourceConfigAdded(DefaultCompositeConfig dcc, Config config) {
            dcc.refreshState();
            dcc.notifyConfigAdded(dcc);
        }

        @Override
        public void onSourceConfigRemoved(DefaultCompositeConfig dcc, Config config) {
            dcc.refreshState();
            dcc.notifyConfigRemoved(dcc);
        }

        @Override
        public void onSourceConfigUpdated(DefaultCompositeConfig dcc, Config config) {
            dcc.refreshState();
            dcc.notifyConfigUpdated(dcc);
        }

        @Override
        public void onSourceError(Throwable error, DefaultCompositeConfig dcc, Config config) {
            dcc.notifyError(error, dcc);
        }
    }
    
    private final ConfigListener listener;
    private final boolean reversed;
    private volatile State state;
    
    public DefaultCompositeConfig() {
        this(false);
    }
    
    public DefaultCompositeConfig(boolean reversed) {
        this.reversed = reversed;
        this.listener = new CompositeConfigListener(this);
        
        this.state = new State(Collections.emptyMap(), 0);
    }

    private void refreshState() {
        this.state = state.refresh();
    }


    @Override
    public synchronized boolean addConfig(String name, Config child) throws ConfigException {
        return internalAddConfig(name, child);
    }
    
    private synchronized boolean internalAddConfig(String name, Config child) throws ConfigException {
        LOG.info("Adding config {} to {}", name, hashCode());
        
        if (child == null) {
            // TODO: Log a warning?
            return false;
        }
        
        if (name == null) {
            throw new ConfigException("Child configuration must be named");
        }
        
        if (state.containsConfig(name)) {
            LOG.info("Configuration with name'{}' already exists", name);
            return false;
        }

        state = state.addConfig(name, child);
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
        return state.children.keySet();
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
        Config child = state.getConfig(name);
        if (child != null) {
            state = state.removeConfig(name);
            child.removeListener(listener);
            this.notifyConfigRemoved(child);
        }
        return child;
    }    

    @Override
    public Config getConfig(String name) {
        return state.children.get(name);
    }


    @Override
    public Object getRawProperty(String key) {
        return state.data.get(key);
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
        return state.data.keySet().iterator();
    }

    @Override
    public synchronized <T> T accept(Visitor<T> visitor) {
        AtomicReference<T> result = new AtomicReference<>(null);
        if (visitor instanceof CompositeVisitor) {
            CompositeVisitor<T> cv = (CompositeVisitor<T>)visitor;
            state.children.forEach((key, config) -> {
                result.set(cv.visitChild(key, config));
            });
        } else {
            state.data.forEach(visitor::visitKey);
        }
        return result.get();
    }

    public static com.netflix.archaius.api.config.CompositeConfig from(LinkedHashMap<String, Config> load) throws ConfigException {
        Builder builder = builder();
        for (Entry<String, Config> config : load.entrySet()) {
            builder.withConfig(config.getKey(), config.getValue());
        }
        return builder.build();
    }
    
    @Override
    public boolean containsKey(String key) {
        return state.data.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return state.data.isEmpty();
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        this.state.data.forEach(consumer);
    }

    @Override
    public String toString() {
        return "[" + String.join(" ", state.children.keySet()) + "]";
    }
}
