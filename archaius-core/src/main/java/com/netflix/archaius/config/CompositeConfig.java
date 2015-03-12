package com.netflix.archaius.config;

import java.util.Collection;

import com.netflix.archaius.Config;
import com.netflix.archaius.exceptions.ConfigException;

public interface CompositeConfig extends Config {
    public static interface Listener {
        void onConfigAdded(Config child);
    }

    public static interface CompositeVisitor {
        void visit(Config child);
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void addConfigLast(Config child) throws ConfigException;

    void addConfigFirst(Config child) throws ConfigException;

    Collection<String> getChildConfigNames();

    void addConfigsLast(Collection<Config> config) throws ConfigException;

    void addConfigsFirst(Collection<Config> config) throws ConfigException;

    boolean replace(Config child);

    void removeConfig(Config child);
    

}
