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
