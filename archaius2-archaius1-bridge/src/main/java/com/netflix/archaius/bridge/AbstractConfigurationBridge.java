package com.netflix.archaius.bridge;

import java.util.ArrayList;
import java.util.HashSet;

import com.netflix.config.util.InstrumentationAware;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.commons.CommonsToConfig;
import com.netflix.archaius.config.DefaultConfigListener;
import com.netflix.archaius.exceptions.ConfigAlreadyExistsException;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.DynamicPropertySupport;
import com.netflix.config.PropertyListener;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @see StaticArchaiusBridgeModule
 */
@Singleton
class AbstractConfigurationBridge extends AbstractConfiguration implements AggregatedConfiguration, DynamicPropertySupport, InstrumentationAware {

    private final Config config;
    private final SettableConfig settable;
    private final CompositeConfig libraries;
    private final AtomicInteger libNameCounter = new AtomicInteger();
    
    {
        AbstractConfiguration.setDefaultListDelimiter('\0');
    }
    
    @Inject
    public AbstractConfigurationBridge(
            final Config config, 
            @LibrariesLayer CompositeConfig libraries, 
            @RuntimeLayer SettableConfig settable) {
        this.config = config;
        this.settable = settable;
        this.libraries = libraries;
        this.config.addListener(new ConfigListener() {
			@Override
			public void onConfigAdded(Config child) {
				onConfigUpdated(config);
			}

			@Override
			public void onConfigRemoved(Config child) {
				onConfigUpdated(config);
			}

			@Override
			public void onConfigUpdated(Config config) {
				fireEvent(HierarchicalConfiguration.EVENT_ADD_NODES, null, null, true);
				fireEvent(HierarchicalConfiguration.EVENT_ADD_NODES, null, null, false);
			}

			@Override
			public void onError(Throwable error, Config config) {
			}
        });
    }
    
    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    @Override
    public Object getProperty(String key) {
        return config.getRawProperty(key);  // Should interpolate
    }

    @Override
    public Object getPropertyUninstrumented(String key) {
        return config.getRawPropertyUninstrumented(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        settable.setProperty(key, value);
    }

    @Override
    protected void clearPropertyDirect(String key) {
        settable.clearProperty(key);
    }

    @Override
    public void addConfiguration(AbstractConfiguration config) {
        addConfiguration(config, "Config-" + libNameCounter.incrementAndGet());
    }

    @Override
    public void addConfiguration(AbstractConfiguration config, String name) {
        try {
            libraries.addConfig(name, new CommonsToConfig(config));
        }
        catch (ConfigAlreadyExistsException e) {
            // OK To ignore
        } 
        catch (ConfigException e) {
            throw new RuntimeException("Unable to add configuration " + name, e);
        }
    }

    @Override
    public Set<String> getConfigurationNames() {
        return new HashSet<>(libraries.getConfigNames());
    }

    @Override
    public List<String> getConfigurationNameList() {
        return new ArrayList<>(libraries.getConfigNames());
    }

    @Override
    public Configuration getConfiguration(String name) {
        return new ConfigToCommonsAdapter(libraries.getConfig(name));
    }

    @Override
    public int getNumberOfConfigurations() {
        return libraries.getConfigNames().size();
    }

    @Override
    public Configuration getConfiguration(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AbstractConfiguration> getConfigurations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration removeConfiguration(String name) {
        libraries.removeConfig(name);
        return null;
    }

    @Override
    public boolean removeConfiguration(Configuration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration removeConfigurationAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConfigurationListener(final PropertyListener expandedPropertyListener) {
        config.addListener(new DefaultConfigListener() {
            @Override
            public void onConfigAdded(Config config) {
                expandedPropertyListener.configSourceLoaded(config);
            }

            @Override
            public void onConfigRemoved(Config config) {
                expandedPropertyListener.configSourceLoaded(config);
            }

            @Override
            public void onConfigUpdated(Config config) {
                expandedPropertyListener.configSourceLoaded(config);
            }
        });
    }

    public Object resolve(String value) {
        return config.resolve(value);
    }
}
