package com.netflix.archaius.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.netflix.config.util.InstrumentationAware;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicPropertySupport;
import com.netflix.config.PropertyListener;

/**
 * @see StaticArchaiusBridgeModule
 */
@Singleton
public class StaticAbstractConfiguration extends AbstractConfiguration implements AggregatedConfiguration, DynamicPropertySupport, InstrumentationAware {
    
    private static volatile AbstractConfigurationBridge delegate;
    
    private static final StaticAbstractConfiguration INSTANCE = new StaticAbstractConfiguration();
    
    @Inject
    public synchronized static void initialize(DeploymentContext context, AbstractConfigurationBridge config) {
        reset();
        delegate = config;
        
        AbstractConfiguration actualConfig = ConfigurationManager.getConfigInstance();
        if (!actualConfig.equals(INSTANCE)) {
            UnsupportedOperationException cause = new UnsupportedOperationException("**** Remove static reference to ConfigurationManager or FastProperty in this call stack ****");
            
            cause.setStackTrace(ConfigurationManager.getStaticInitializationSource());
            throw new IllegalStateException("Not using expected bridge!!! " + actualConfig.getClass() + " instead of " + StaticAbstractConfiguration.class, cause);
        }
        
        DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration)INSTANCE);

        // Bridge change notification from the new delegate to any listeners registered on 
        // this static class.  Notifications will be removed if the StaticAbstractConfiguration 
        // is reset and will reattached to a new bridge should initialize be called again.
        config.addConfigurationListener(INSTANCE.forwardingConfigurationListener);
        config.addConfigurationListener(INSTANCE.forwardingPropertyListener);
    }
    
    public static AbstractConfiguration getInstance() {
    	 return INSTANCE;
    }
    
    public synchronized static void reset() {
    	if (delegate != null) {
    		delegate.removeConfigurationListener(INSTANCE.forwardingConfigurationListener);
    	}
        delegate = null;
    }

    private final PropertyListener forwardingPropertyListener;
    private final ConfigurationListener forwardingConfigurationListener;
    private final CopyOnWriteArrayList<PropertyListener> propertyListeners = new CopyOnWriteArrayList<>();

    public StaticAbstractConfiguration() {
        this.forwardingPropertyListener = new PropertyListener() {
    		@Override
    		public void configSourceLoaded(Object source) {
    			propertyListeners.forEach(listener -> listener.configSourceLoaded(source));
    		}

    		@Override
    		public void addProperty(Object source, String name, Object value, boolean beforeUpdate) {
    			propertyListeners.forEach(listener -> listener.addProperty(source, name, value, beforeUpdate));
    		}

    		@Override
    		public void setProperty(Object source, String name, Object value, boolean beforeUpdate) {
    			propertyListeners.forEach(listener -> listener.setProperty(source, name, value, beforeUpdate));
    		}

    		@Override
    		public void clearProperty(Object source, String name, Object value, boolean beforeUpdate) {
    			propertyListeners.forEach(listener -> listener.clearProperty(source, name, value, beforeUpdate));
    		}

    		@Override
    		public void clear(Object source, boolean beforeUpdate) {
    			propertyListeners.forEach(listener -> listener.clear(source, beforeUpdate));
    		}
        	
        };
        
        this.forwardingConfigurationListener = new ConfigurationListener() {
    		@Override
    		public void configurationChanged(ConfigurationEvent event) {
    			StaticAbstractConfiguration.this.fireEvent(event.getType(), event.getPropertyName(), event.getPropertyValue(), event.isBeforeUpdate());
    		}
        };    	
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
    public Object getPropertyUninstrumented(String key) {
        if (delegate == null) {
            System.out.println(
                    "[getPropertyUninstrumented(" + key + ")] StaticAbstractConfiguration not initialized yet.");
            return null;
        }
        return delegate.getPropertyUninstrumented(key);
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
    protected String interpolate(String base) {
        return delegate.resolve(base).toString();
    }

    @Override
    protected Object interpolate(Object value) {
        return delegate.resolve(value.toString());
    }

    @Override
    protected void clearPropertyDirect(String key) {
        delegate.clearProperty(key);
    }

    public Collection<ConfigurationListener> getConfigurationListeners() {
    	List<ConfigurationListener> listeners = new ArrayList<>(super.getConfigurationListeners());
    	Optional.ofNullable(delegate).ifPresent(d -> listeners.addAll(d.getConfigurationListeners()));
    	return listeners;
	}

	@Override
	public void addConfigurationListener(PropertyListener expandedPropertyListener) {
		propertyListeners.add(expandedPropertyListener);
	}
}
