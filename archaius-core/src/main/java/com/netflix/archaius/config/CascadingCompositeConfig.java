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
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import com.netflix.archaius.Config;
import com.netflix.archaius.Decoder;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.exceptions.ConfigException;

/**
 * Config that is a composite of multiple configuration and as such doesn't track a 
 * configuration of its own.  The composite does not merge the configurations but instead
 * treats them as overrides so that a property existing in a configuration supersedes
 * the same property in configuration that was added later.
 * 
 * @author elandau
 *
 * TODO: Optional cache of queried properties
 */
public class CascadingCompositeConfig extends DelegatingConfig implements CompositeConfig {
    private final CopyOnWriteArrayList<Config>  children  = new CopyOnWriteArrayList<Config>();
    private final Map<String, Config>           lookup    = new LinkedHashMap<String, Config>();
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<Listener>();
    private Decoder decoder;
    
    public CascadingCompositeConfig(String name) {
        super(name);
        decoder = new DefaultDecoder();
    }

    protected void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }
    
    @Override
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }
    
    public void notifyOnAddConfig(Config child) {
        for (Listener listener : listeners) {
            listener.onConfigAdded(child);
        }
    }
    
    /**
     * Add a Config to the end of the list so that it has least priority
     * @param child
     * @return
     */
    @Override
    public synchronized void addConfigLast(Config child) throws ConfigException {
        if (child == null) {
            return;
        }
        if (lookup.containsKey(child.getName())) {
            throw new ConfigException("Configuration with name " + child.getName() + " already exists");
        }

        lookup.put(child.getName(), child);
        children.add(child);
        postConfigAdd(child);
    }
    
    /**
     * Add a Config to the end of the list so that it has highest priority
     * @param child
     * @return
     */
    @Override
    public synchronized void addConfigFirst(Config child) throws ConfigException {
        if (child == null) {
            return;
        }
        if (lookup.containsKey(child.getName())) {
            throw new ConfigException("Configuration with name " + child.getName() + " already exists");
        }
        lookup.put(child.getName(), child);

        children.add(0, child);
        postConfigAdd(child);
    }
    
    @Override
    public synchronized Collection<String> getChildConfigNames() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.lookup.keySet());
        return result;
    }
    
    protected void postConfigAdd(Config child) {
        child.setStrInterpolator(this.getStrInterpolator());
        child.setParent(this);
        notifyOnAddConfig(child);
    }
    
    @Override
    public void addConfigsLast(Collection<Config> config) throws ConfigException {
        for (Config child : config) {
            addConfigLast(child);
        }
    }
    
    @Override
    public void addConfigsFirst(Collection<Config> config) throws ConfigException {
        for (Config child : config) {
            addConfigFirst(child);
        }
    }
    
    @Override
    public synchronized boolean replace(Config child) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getName().equals(child.getName())) {
                children.set(i, child);
                postConfigAdd(child);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public synchronized void removeConfig(Config child) {
        if (this.children.remove(child)) {
            this.lookup.remove(child.getName());
        }
    }    
    
    protected Config getConfigWithProperty(String key, boolean failOnNotFound) {
        for (Config child : children) {
            if (child.containsProperty(key)) {
                return child;
            }
        }
        
        if (failOnNotFound)
            throw new NoSuchElementException("No child configuration has property " + key);
        return null;
    }

    @Override
    public boolean containsProperty(String key) {
        for (Config child : children) {
            if (child.containsProperty(key)) {
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
        sb.append(getName()).append("[");
        
        for (Config child : children) {
            sb.append(child).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof CompositeVisitor) {
            CompositeVisitor cv = (CompositeVisitor)visitor;
            for (Config child : children) {
                cv.visit(child);
            }
        }
    }

}
