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

import java.util.Collection;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicPropertyFactory {
    
    private static DynamicPropertyFactory instance = new DynamicPropertyFactory();
    private volatile static DynamicPropertySupport config = null;
    private volatile static boolean initializedWithDefaultConfig = false;    
    private static final Logger logger = LoggerFactory.getLogger(DynamicPropertyFactory.class);
    public static final String THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION = 
        "dynamicProperty.throwMissingConfigurationSourceException";
    private volatile static boolean throwMissingConfigurationSourceException = 
        Boolean.getBoolean(THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION);

    private DynamicPropertyFactory() {}
            
    public static synchronized DynamicPropertyFactory initWithConfigurationSource(AbstractConfiguration config) {
        if (config instanceof DynamicPropertySupport) {
            return initWithConfigurationSource((DynamicPropertySupport) config);    
        }
        return initWithConfigurationSource(new ConfigurationBackedDynamicPropertySupportImpl(config));
    }
    
    public static void setThrowMissingConfigurationSourceException(boolean value) {
        throwMissingConfigurationSourceException = value;
    }
    
    public static boolean isThrowMissingConfigurationSourceException() {
        return throwMissingConfigurationSourceException;
    }
        
    public static synchronized DynamicPropertyFactory initWithConfigurationSource(DynamicPropertySupport dynamicPropertySupport) {
        if (dynamicPropertySupport == null) {
            throw new IllegalArgumentException("dynamicPropertySupport is null");
        }
        // If the factory is initialized with default configuration, we need to change that
        if (initializedWithDefaultConfig && (config instanceof ConfigurationBackedDynamicPropertySupportImpl)) {
            DynamicURLConfiguration defaultFileConfig = (DynamicURLConfiguration) ((ConfigurationBackedDynamicPropertySupportImpl) config).getConfiguration();
            // stop loading of the configuration
            defaultFileConfig.stopLoading();
            Collection<ConfigurationListener> listeners = defaultFileConfig.getConfigurationListeners();
            
            // find the listener and remove it so that DynamicProperty will no longer receives 
            // callback from the default configuration source
            ConfigurationListener dynamicPropertyListener = null;
            for (ConfigurationListener l: listeners) {
                if (l instanceof ExpandedConfigurationListenerAdapter
                        && ((ExpandedConfigurationListenerAdapter) l).getListener() 
                        instanceof DynamicProperty.DynamicPropertyListener) {
                    dynamicPropertyListener = l;
                    break;                        
                }
            }
            if (dynamicPropertyListener != null) {
                defaultFileConfig.removeConfigurationListener(dynamicPropertyListener);
            }
            config = null;
        }
        if (config != null && config != dynamicPropertySupport) {
            throw new IllegalStateException("DynamicPropertyFactory is already initialized with a diffrerent configuration source: " + config);
        }
        config = dynamicPropertySupport;
        DynamicProperty.registerWithDynamicPropertySupport(dynamicPropertySupport);
        initializedWithDefaultConfig = false;
        return instance;
    }
    
    public static DynamicPropertyFactory getInstance() throws MissingConfigurationSourceException {
        if (config == null) {
            synchronized (DynamicPropertyFactory.class) {
                if (config == null ) {
                    try {
                        DynamicURLConfiguration defaultConfig = new DynamicURLConfiguration();
                        initWithConfigurationSource(defaultConfig);
                        initializedWithDefaultConfig = true;
                        if (!Boolean.getBoolean("dynamicPropertyFactory.disableLogging")) {
                            logger.info("DynamicPropertyFactory is initialized with default configuration source(s): " + defaultConfig.getSource());
                        }
                    } catch (Throwable e) {
                        if (isThrowMissingConfigurationSourceException()) {
                            throw new MissingConfigurationSourceException("Error initializing with default configuration source(s).", e);
                        } else {
                            logger.warn("Error initializing with default configuration source(s).", e);
                        }
                    }
                }
            }
        }
        return instance;
    }
                    
    private static void checkAndWarn(String propName) {
        if (config == null && !Boolean.getBoolean("dynamicPropertyFactory.disableLogging")) {
            logger.warn("DynamicProperty " + propName + " is created without a configuration source for callback. " 
                    + "Need to set property " + URLConfigurationSource.CONFIG_URL + " or call DynamicPropertyFactory.initWithConfigurationSource().");
        }        
    }

    public DynamicStringProperty createStringProperty(String propName, String defaultValue) {
        return createStringProperty(propName, defaultValue, null);
    }

    public DynamicStringProperty createStringProperty(String propName, String defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicStringProperty property = new DynamicStringProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    public DynamicIntProperty createIntProperty(String propName, int defaultValue) {
        return createIntProperty(propName, defaultValue, null);
    }

    public DynamicIntProperty createIntProperty(String propName, int defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicIntProperty property = new DynamicIntProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    public DynamicLongProperty createLongProperty(String propName, long defaultValue) {
        return createLongProperty(propName, defaultValue, null);
    }

    public DynamicLongProperty createLongProperty(String propName, long defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicLongProperty property = new DynamicLongProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }
    
    public DynamicBooleanProperty createBooleanProperty(String propName, boolean defaultValue) {
        return createBooleanProperty(propName, defaultValue, null);
    }

    public DynamicBooleanProperty createBooleanProperty(String propName, boolean defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicBooleanProperty property = new DynamicBooleanProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }
    
    public DynamicFloatProperty createFloatProperty(String propName, float defaultValue) {
        return createFloatProperty(propName, defaultValue, null);
    }

    public DynamicFloatProperty createFloatProperty(String propName, float defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicFloatProperty property = new DynamicFloatProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    public DynamicDoubleProperty createDoubleProperty(String propName, double defaultValue) {
        return createDoubleProperty(propName, defaultValue, null);
    }
    
    public DynamicDoubleProperty createDoubleProperty(String propName, double defaultValue,  final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicDoubleProperty property = new DynamicDoubleProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }
    
    private static void addCallback(Runnable callback, PropertyWrapper<?> wrapper) {
        if (callback != null) {
            wrapper.prop.addCallback(callback);
        }
    }

}
