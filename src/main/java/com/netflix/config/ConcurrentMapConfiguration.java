/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

public class ConcurrentMapConfiguration extends AbstractConfiguration {
    private final Map<String,Object> props = new ConcurrentHashMap<String,Object>();
    
    public ConcurrentMapConfiguration() {
    }
    
    public ConcurrentMapConfiguration(Configuration config) {
        for (Iterator i = config.getKeys(); i.hasNext();) {
            String name = (String) i.next();
            Object value = config.getProperty(name);
            addPropertyDirect(name, value);
        }
    }
    
    @Override
    protected final void addPropertyDirect(String key, Object value) {
        props.put(key, value);
    }
    
    @Override
    protected void clearPropertyDirect(String key)
    {
       props.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
       return props.containsKey(key);
    }

    @Override
    public Iterator getKeys() {        
        return props.keySet().iterator();
    }

    @Override
    public Object getProperty(String key) {
        return props.get(key);
    }

    @Override
    public boolean isEmpty() { 
        return props.isEmpty();
    }
}
