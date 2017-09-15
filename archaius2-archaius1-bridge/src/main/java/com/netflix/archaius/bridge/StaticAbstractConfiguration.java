package com.netflix.archaius.bridge;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.event.ConfigurationListener;

import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicPropertySupport;
import com.netflix.config.PropertyListener;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @see StaticArchaiusBridgeModule
 */
@Singleton
public class StaticAbstractConfiguration extends AbstractConfiguration implements AggregatedConfiguration, DynamicPropertySupport {
    
    private static volatile AbstractConfigurationBridge delegate;
    private static ConcurrentLinkedQueue<PropertyListener> pendingListeners = new ConcurrentLinkedQueue<>();
    
    private static final StaticAbstractConfiguration INSTANCE = new StaticAbstractConfiguration();
    
    @Inject
    public static void initialize(DeploymentContext context, AbstractConfigurationBridge config) {
        delegate = config;
        
        AbstractConfiguration actualConfig = ConfigurationManager.getConfigInstance();
        if (!actualConfig.equals(INSTANCE)) {
            UnsupportedOperationException cause = new UnsupportedOperationException("**** Remove static reference to ConfigurationManager or FastProperty in this call stack ****");
            
            cause.setStackTrace(ConfigurationManager.getStaticInitializationSource());
            throw new IllegalStateException("Not using expected bridge!!! " + actualConfig.getClass() + " instead of " + StaticAbstractConfiguration.class, cause);
        }
        
        DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration)INSTANCE);
        
        PropertyListener listener;
        while (null != (listener = pendingListeners.poll())) {
            delegate.addConfigurationListener(listener);
        }
    }
    
    public static AbstractConfiguration getInstance() {
    	 return INSTANCE;
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
    public String getString(String key, String defaultValue) {
        if (delegate == null) {
            System.out.println("[getString(" + key + ", " + defaultValue + ")] StaticAbstractConfiguration not initialized yet.");
            return defaultValue;
        }
         return delegate.getString(key, defaultValue);
    }

    @Override
    public Object getProperty(String key) {
        if (delegate == null) {
            System.out.println("[getProperty(" + key + ")] StaticAbstractConfiguration not initialized yet.");
            return null;
        }
        return delegate.getProperty(key);
    }

    @Override
    public Iterator<String> getKeys() {
        if (delegate == null) {
            throw new RuntimeException("[getKeys()] StaticAbstractConfiguration not initialized yet.");
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
    protected void clearPropertyDirect(String key) {
        delegate.clearProperty(key);
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
    
    public void addConfigurationListener(ConfigurationListener l) {
        delegate.addConfigurationListener(l);
    }

    public boolean removeConfigurationListener(ConfigurationListener l) {
        return delegate.removeConfigurationListener(l);
    }

    public Collection<ConfigurationListener> getConfigurationListeners() {
        return getConfigurationListeners();
    }
}
