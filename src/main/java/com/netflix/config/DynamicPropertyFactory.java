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

/**
 * A factory that creates instances of dynamic properties and associates them with an underlying configuration 
 * or {@link DynamicPropertySupport} where the properties could be changed dynamically at runtime.
 * <p>
 * It is recommended to initialize this class with a configuration or DynamicPropertySupport before the first call to
 * {@link #getInstance()}. Otherwise, it will be initialized with a default {@link DynamicURLConfiguration} where 
 * the configuration source URLs are defined by system properties.  
 * <p>
 * Example:<pre>
 *    import com.netflix.config.DynamicProperty;
 *
 *    class MyClass {
 *        private static DynamicIntProperty maxWaitMillis
 *            = DynamicPropertyFactory.getInstance().createIntProperty("myclass.sleepMillis", 250);
 *           // ...
 *
 *           // Wait for a configurable amount of time for condition to become true.
 *           // Note that the time can be changed on-the-fly.
 *           someCondition.wait(maxWaitMillis.get());
 *
 *        // ...
 *    }
 * </pre>
 * <p>
 * Please note that you should not cache the property value if you expect the value to change on-the-fly. For example,
 * in the following code the change of the value is ineffective:
 * <p> 
 *<pre>
 *    int maxWaitMillis = DynamicPropertyFactory.getInstance().createIntProperty("myclass.sleepMillis", 250).get();
 *    // ...
 *    someCondition.wait(maxWaitMillis);
 *</pre>
 * @author awang
 *
 */
public class DynamicPropertyFactory {
    
    private static DynamicPropertyFactory instance = new DynamicPropertyFactory();
    private volatile static DynamicPropertySupport config = null;
    private volatile static boolean initializedWithDefaultConfig = false;    
    private static final Logger logger = LoggerFactory.getLogger(DynamicPropertyFactory.class);
    
    /**
     * System property name that defines whether {@link #getInstance()} should throw 
     * {@link MissingConfigurationSourceException} if there is no proper configuration source
     * at the time of call.
     */
    public static final String THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION = 
        "dynamicProperty.throwMissingConfigurationSourceException";
    private volatile static boolean throwMissingConfigurationSourceException = 
        Boolean.getBoolean(THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION);

    private DynamicPropertyFactory() {}
            
    /**
     * Initialize the factory with an AbstractConfiguration. 
     * <p>
     * The initialization will register a ConfigurationListener with the configuration so that {@link DynamicProperty} 
     * will receives a callback and refresh its value when a property is changed in the configuration.
     * <p>
     * If the factory is already initialized with a default configuration source (see {@link #getInstance()}), it will re-register
     * itself with the new configuration source passed to this method. Otherwise, this method will throw IllegalArgumentException
     * if it has been initialized with a different and non-default configuration source. This method should be only called once. 
     *
     * @param config Configuration to be attached with DynamicProperty   
     * @return the instance of DynamicPropertyFactory
     * @throws IllegalArgumentException if the factory has already been initialized with a non-default configuration source
     */
    public static synchronized DynamicPropertyFactory initWithConfigurationSource(AbstractConfiguration config) {
        if (config instanceof DynamicPropertySupport) {
            return initWithConfigurationSource((DynamicPropertySupport) config);    
        }
        return initWithConfigurationSource(new ConfigurationBackedDynamicPropertySupportImpl(config));
    }
    
    /**
     * Set the boolean value to indicate whether {@link #getInstance()} should throw 
     * {@link MissingConfigurationSourceException} if there is no proper configuration source
     * at the time of call.
     * 
     * @param value to set
     */
    public static void setThrowMissingConfigurationSourceException(boolean value) {
        throwMissingConfigurationSourceException = value;
    }
    
    /**
     * @return the boolean value to indicate whether {@link #getInstance()} should throw 
     * {@link MissingConfigurationSourceException} if there is no proper configuration source
     * at the time of call.
     */
    public static boolean isThrowMissingConfigurationSourceException() {
        return throwMissingConfigurationSourceException;
    }
        
    /**
     * Initialize the factory with a {@link DynamicPropertySupport}. 
     * <p>
     * The initialization will register a {@link PropertyListener} with the DynamicPropertySupport so that DynamicProperty 
     * will receives a callback and refresh its value when a property is changed.
     * <p>
     * If the factory is already initialized with a default configuration source (see {@link #getInstance()}), it will re-register
     * itself with the new configuration source passed to this method. Otherwise, this method will throw IllegalArgumentException
     * if it has been initialized with a non-default configuration source. This method should be only called once. 
     * 
     * @param dynamicPropertySupport DynamicPropertySupport to be associated with the DynamicProperty
     * @return the instance of DynamicPropertyFactory
     * @throws IllegalArgumentException if the factory has already been initialized with a different and non-default configuration source
     */
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
    
    /**
     * Get the instance to create dynamic properties. If the factory is not initialized with a configuration source 
     * (see {@link #initWithConfigurationSource(AbstractConfiguration)} and {@link #initWithConfigurationSource(DynamicPropertySupport)}),
     * it will fist try to initialize itself with a default {@link DynamicURLConfiguration}, which at a fixed interval polls 
     * a configuration file (see {@link URLConfigurationSource#DEFAULT_CONFIG_FILE_NAME} on classpath and a set of URLs specified via a system property
     * (see {@link URLConfigurationSource#CONFIG_URL}).
     * 
     * @return
     * @throws MissingConfigurationSourceException
     */
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

    /**
     * Create a new property whose value is a string and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicStringProperty createStringProperty(String propName, String defaultValue) {
        return createStringProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a string and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicStringProperty createStringProperty(String propName, String defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicStringProperty property = new DynamicStringProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is an integer and subject to change on-the-fly..
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicIntProperty createIntProperty(String propName, int defaultValue) {
        return createIntProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is an integer and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicIntProperty createIntProperty(String propName, int defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicIntProperty property = new DynamicIntProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a long and subject to change on-the-fly..
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicLongProperty createLongProperty(String propName, long defaultValue) {
        return createLongProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a long and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicLongProperty createLongProperty(String propName, long defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicLongProperty property = new DynamicLongProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }
    
    /**
     * Create a new property whose value is a boolean and subject to change on-the-fly..
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicBooleanProperty createBooleanProperty(String propName, boolean defaultValue) {
        return createBooleanProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a boolean and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicBooleanProperty createBooleanProperty(String propName, boolean defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicBooleanProperty property = new DynamicBooleanProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }
    
    /**
     * Create a new property whose value is a float and subject to change on-the-fly..
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicFloatProperty createFloatProperty(String propName, float defaultValue) {
        return createFloatProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a float and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicFloatProperty createFloatProperty(String propName, float defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicFloatProperty property = new DynamicFloatProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a double and subject to change on-the-fly..
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicDoubleProperty createDoubleProperty(String propName, double defaultValue) {
        return createDoubleProperty(propName, defaultValue, null);
    }
    
    /**
     * Create a new property whose value is a double and subject to change on-the-fly.
     *  
     * @param propName property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
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
