package com.netflix.archaius.typesafe;

import java.util.Iterator;
import java.util.Map.Entry;

import com.netflix.archaius.config.AbstractConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class TypesafeConfig extends AbstractConfig {

    private final Config config;
    
    public TypesafeConfig(String name, Config config) {
        super(name);
        this.config = config;
    }

    @Override
    public boolean containsProperty(String key) {
        return config.hasPath(key);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        return config.getString(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return new Iterator<String>() {
            Iterator<Entry<String, ConfigValue>> iter = config.entrySet().iterator();
                    
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return iter.next().getKey();
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }
}
