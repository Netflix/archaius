package com.netflix.archaius.bridge;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicPropertySupport;
import com.netflix.config.PropertyListener;

/**
 * @see StaticArchaiusBridgeModule
 * @author elandau
 */
public class StaticAbstractConfiguration extends AbstractConfiguration implements AggregatedConfiguration, DynamicPropertySupport {
    
    private static volatile AbstractConfigurationBridge delegate;
    private static ConcurrentLinkedQueue<PropertyListener> pendingListeners = new ConcurrentLinkedQueue<>();
    private static StaticAbstractConfiguration staticConfig;
    
    public StaticAbstractConfiguration() {
        staticConfig = this;
    }
    
    @Inject
    public static void intialize(AbstractConfigurationBridge config) {
        delegate = config;

        if (staticConfig == null) {
            System.err.println("Trying to use bridge but but hasn't been configured yet!!!");
            return;
        }
        
        AbstractConfiguration actualConfig = ConfigurationManager.getConfigInstance();
        if (!actualConfig.equals(staticConfig)) {
            System.err.println("Not using expected bridge!!!");
            return;
        }
        
        DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration)staticConfig);
        PropertyListener listener;
        while (null != (listener = pendingListeners.poll())) {
            delegate.addConfigurationListener(listener);
        }
    }

    public static void reset() {
        delegate = null;
    }

    @Override
    public boolean isEmpty() {
        if (delegate == null) {
            System.err.println("[isEmpty()] StaticAbstractConfiguration not initialized yet.");
            return true;
        }
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        if (delegate == null) {
            System.err.println("[containsKey(" + key + ")] StaticAbstractConfiguration not initialized yet.");
            return false;
        }
        return delegate.containsKey(key);
    }

    @Override
    public Object getProperty(String key) {
        if (delegate == null) {
            System.err.println("[getProperty(" + key + ")] StaticAbstractConfiguration not initialized yet.");
            return null;
        }
        return delegate.getProperty(key);
    }

    @Override
    public Iterator<String> getKeys() {
        if (delegate == null) {
            System.err.println("[getKeys()] StaticAbstractConfiguration not initialized yet.");
            return Collections.emptyIterator();
        }
        return delegate.getKeys();
    }

    @Override
    public void addConfiguration(AbstractConfiguration config) {
        delegate.addConfiguration(config);
    }

    @Override
    public void addConfiguration(AbstractConfiguration config, String name) {
        delegate.addConfiguration(config, name);
    }

    @Override
    public Set<String> getConfigurationNames() {
        return delegate.getConfigurationNames();
    }

    @Override
    public List<String> getConfigurationNameList() {
        return delegate.getConfigurationNameList();
    }

    @Override
    public Configuration getConfiguration(String name) {
        return delegate.getConfiguration(name);
    }

    @Override
    public int getNumberOfConfigurations() {
        return delegate.getNumberOfConfigurations();
    }

    @Override
    public Configuration getConfiguration(int index) {
        return delegate.getConfiguration(index);
    }

    @Override
    public List<AbstractConfiguration> getConfigurations() {
        return delegate.getConfigurations();
    }

    @Override
    public Configuration removeConfiguration(String name) {
        return delegate.removeConfiguration(name);
    }

    @Override
    public boolean removeConfiguration(Configuration config) {
        return delegate.removeConfiguration(config);
    }

    @Override
    public Configuration removeConfigurationAt(int index) {
        return delegate.removeConfigurationAt(index);
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        delegate.addPropertyDirect(key, value);
    }

    @Override
    public void addConfigurationListener(PropertyListener expandedPropertyListener) {
        if (delegate == null) {
            pendingListeners.add(expandedPropertyListener);
        }
        else {  
            delegate.addConfigurationListener(expandedPropertyListener);
        }
    }
}
