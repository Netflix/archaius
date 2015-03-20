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

import com.netflix.archaius.Config;
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
    
    public CascadingCompositeConfig(String name) {
        super(name);
    }

    /**
     * Add a Config to the end of the list so that it has least priority
     * @param child
     * @return
     */
    @Override
    public synchronized void addConfig(Config child) throws ConfigException {
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
    public synchronized Collection<String> getConfigNames() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.lookup.keySet());
        return result;
    }
    
    protected void postConfigAdd(Config child) {
        child.setStrInterpolator(getStrInterpolator());
        child.setDecoder(getDecoder());
        notifyConfigAdded(child);
        child.addListener(new ForwardingConfigListener(getListeners(), this));
    }
    
    @Override
    public void addConfigs(Collection<Config> config) throws ConfigException {
        for (Config child : config) {
            addConfig(child);
        }
    }
    
    @Override
    public synchronized void replaceConfig(Config child) throws ConfigException {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getName().equals(child.getName())) {
                children.set(i, child);
                postConfigAdd(child);
                return;
            }
        }
        addConfig(child);
    }
    
    @Override
    public synchronized boolean removeConfig(Config child) {
        if (this.children.remove(child)) {
            this.lookup.remove(child.getName());
            return true;
        }
        return false;
    }    
    
    protected Config getConfigWithProperty(String key, boolean failOnNotFound) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child;
            }
        }
        
        if (failOnNotFound)
            throw new NoSuchElementException("No child configuration has property " + key);
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
        else {
            for (Config child : children) {
                child.accept(visitor);
            }
        }
    }
}
