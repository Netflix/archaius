package com.netflix.archaius.bridge;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.commons.CommonsToConfig;
import com.netflix.archaius.config.DefaultConfigListener;
import com.netflix.archaius.exceptions.ConfigAlreadyExistsException;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicPropertySupport;
import com.netflix.config.PropertyListener;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @see StaticArchaiusBridgeModule
 * @author elandau
 */
@Singleton
public class StaticAbstractConfiguration extends AbstractConfiguration implements AggregatedConfiguration, DynamicPropertySupport {
    
    private static ConcurrentLinkedQueue<PropertyListener> pendingListeners = new ConcurrentLinkedQueue<>();
    private static StaticAbstractConfiguration staticConfig;

    private final Config config;
    private final SettableConfig settable;
    private final CompositeConfig libraries;
    private final AtomicInteger libNameCounter = new AtomicInteger();

    {
        AbstractConfiguration.setDefaultListDelimiter('\0');
    }

    public static AbstractConfiguration getInstance() {
        if (staticConfig == null) {
            throw new RuntimeException("Do no call ConfigurationManager.getConfigInstance() before static injection of StaticAbstractConfiguration.");
        }

        return staticConfig;
    }
    
    @Inject
    public static void initialize(DeploymentContext context, StaticAbstractConfiguration config) {
        
        // Force archaius1 to initialize, if not already done, which will trigger the above constructor.
        ConfigurationManager.getConfigInstance();
        
        // Additional check to make sure archaius actually created the bridge.
        if (staticConfig == null) {
            UnsupportedOperationException cause = new UnsupportedOperationException("**** Remove static reference to ConfigurationManager or FastProperty in this call stack ****");
            cause.setStackTrace(ConfigurationManager.getStaticInitializationSource());
            throw new IllegalStateException("Archaius2 bridge not usable because ConfigurationManager was initialized too early.  See stack trace below.", cause);
        }
        
        AbstractConfiguration actualConfig = ConfigurationManager.getConfigInstance();
        if (!actualConfig.equals(staticConfig)) {
            UnsupportedOperationException cause = new UnsupportedOperationException("**** Remove static reference to ConfigurationManager or FastProperty in this call stack ****");
            cause.setStackTrace(ConfigurationManager.getStaticInitializationSource());
            throw new IllegalStateException("Not using expected bridge!!! " + actualConfig.getClass() + " instead of " + staticConfig.getClass() + ".  See stack trace below.", cause);
        }
        
        DynamicPropertyFactory.initWithConfigurationSource((AbstractConfiguration)staticConfig);
        PropertyListener listener;
        while (null != (listener = pendingListeners.poll())) {
            staticConfig.addConfigurationListener(listener);
        }
    }

    public static void reset() {
        staticConfig = null;
    }

    @Inject
    public StaticAbstractConfiguration(
            final Config config, 
            @LibrariesLayer CompositeConfig libraries, 
            @RuntimeLayer SettableConfig settable, 
            DeploymentContext context) {
        staticConfig = this;
        
        this.config = config;
        this.settable = settable;
        this.libraries = libraries;
        
        config.addListener(new DefaultConfigListener() {
            @Override
            public void onConfigAdded(Config config) {
                onConfigUpdated(config);
            }

            @Override
            public void onConfigRemoved(Config config) {
                onConfigUpdated(config);
            }

            @Override
            public void onConfigUpdated(Config config) {
                fireEvent(HierarchicalConfiguration.EVENT_ADD_NODES, null, null, true);
                fireEvent(HierarchicalConfiguration.EVENT_ADD_NODES, null, null, false);
            }            
        });
    }

    @Override
    public void addConfigurationListener(PropertyListener expandedPropertyListener) {
        if (staticConfig == null) {
            pendingListeners.add(expandedPropertyListener);
        } else {  
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
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        settable.setProperty(key, value);
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
        return Sets.newHashSet(libraries.getConfigNames());
    }

    @Override
    public List<String> getConfigurationNameList() {
        return Lists.newArrayList(libraries.getConfigNames());
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
    protected void clearPropertyDirect(String key) {
        settable.clearProperty(key);
    }

    @Override
    public Configuration removeConfigurationAt(int index) {
        return null;
    }
}
