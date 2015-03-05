/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config;


import com.netflix.config.jmx.BaseConfigMBean;
import com.netflix.config.sources.URLConfigurationSource;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory that creates instances of dynamic properties and associates them with an underlying configuration
 * or {@link DynamicPropertySupport} where the properties could be changed dynamically at runtime.
 * <p/>
 * It is recommended to initialize this class with a configuration or DynamicPropertySupport before the first call to
 * {@link #getInstance()}. Otherwise, it will be lazily initialized with a {@link ConcurrentCompositeConfiguration},
 * where a SystemConfiguration and {@link DynamicURLConfiguration} will be added. You can also disable installing the default configuration
 * by setting system property {@value #DISABLE_DEFAULT_CONFIG} to be <code>true</code>.
 * <p/>
 * If system property {@value #ENABLE_JMX} is set to <code>true</code>, when this class is initialized with a configuration,
 * the configuration will also be exposed to JMX via an instance of {@link BaseConfigMBean}, where you can update the properties
 * via jconsole.
 * <p/>
 * Example:<pre>
 *    import com.netflix.config.DynamicProperty;
 * <p/>
 *    class MyClass {
 *        private static DynamicIntProperty maxWaitMillis
 *            = DynamicPropertyFactory.getInstance().getIntProperty("myclass.sleepMillis", 250);
 *           // ...
 * <p/>
 *           // add a callback when this property is changed
 *           maxWaitMillis.addCallback(new Runnable() {
 *               public void run() {
 *                   int currentValue = maxWaitMillis.get();
 *                   // ...
 *               }
 *           });
 *           // ...
 *           // Wait for a configurable amount of time for condition to become true.
 *           // Note that the time can be changed on-the-fly.
 *           someCondition.wait(maxWaitMillis.get());
 * <p/>
 *        // ...
 *    }
 * </pre>
 * <p/>
 * Please note that you should not cache the property value if you expect the value to change on-the-fly. For example,
 * in the following code the change of the value is ineffective:
 * <p/>
 * <pre>
 *    int maxWaitMillis = DynamicPropertyFactory.getInstance().getIntProperty("myclass.sleepMillis", 250).get();
 *    // ...
 *    someCondition.wait(maxWaitMillis);
 * </pre>
 *
 * @author awang
 */
public class DynamicPropertyFactory {

    private static DynamicPropertyFactory instance = new DynamicPropertyFactory();
    private volatile static DynamicPropertySupport config = null;
    private volatile static boolean initializedWithDefaultConfig = false;
    private static final Logger logger = LoggerFactory.getLogger(DynamicPropertyFactory.class);

    /**
     * @deprecated Moved to ConfigurationManager in 0.5.12
     */
    @Deprecated
    public static final String URL_CONFIG_NAME = ConfigurationManager.URL_CONFIG_NAME;

    /**
     * @deprecated Moved to ConfigurationManager in 0.5.12
     */
    @Deprecated
    public static final String SYS_CONFIG_NAME = ConfigurationManager.SYS_CONFIG_NAME;

    /**
     * Boolean system property to define whether a configuration MBean should be registered with
     * JMX so that properties can be accessed via JMX console. Default is "unset".
     */
    public static final String ENABLE_JMX = "archaius.dynamicPropertyFactory.registerConfigWithJMX";


    /**
     * System property name that defines whether {@link #getInstance()} should throw
     * {@link MissingConfigurationSourceException} if there is no proper configuration source
     * at the time of call.
     */
    public static final String THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION =
            "archaius.dynamicProperty.throwMissingConfigurationSourceException";
    private volatile static boolean throwMissingConfigurationSourceException =
            Boolean.getBoolean(THROW_MISSING_CONFIGURATION_SOURCE_EXCEPTION);

    /**
     * System property to disable adding SystemConfiguration to the default ConcurrentCompositeConfiguration
     *
     * @deprecated Moved to ConfigurationManager in 0.5.12
     */
    @Deprecated
    public static final String DISABLE_DEFAULT_SYS_CONFIG = ConfigurationManager.DISABLE_DEFAULT_SYS_CONFIG;

    /**
     * System property to determine whether DynamicPropertyFactory should be lazily initialized with
     * default configuration for {@link #getInstance()}. Default is false (not set).
     */
    public static final String DISABLE_DEFAULT_CONFIG = "archaius.dynamicProperty.disableDefaultConfig";

    private DynamicPropertyFactory() {
    }

    /**
     * Initialize the factory with an AbstractConfiguration.
     * <p/>
     * The initialization will register a ConfigurationListener with the configuration so that {@link DynamicProperty}
     * will receives a callback and refresh its value when a property is changed in the configuration.
     * <p/>
     * If the factory is already initialized with a default configuration source (see {@link #getInstance()}), it will re-register
     * itself with the new configuration source passed to this method. Otherwise, this method will throw IllegalArgumentException
     * if it has been initialized with a different and non-default configuration source. This method should be only called once.
     *
     * @param config Configuration to be attached with DynamicProperty
     * @return the instance of DynamicPropertyFactory
     * @throws IllegalArgumentException if the factory has already been initialized with a non-default configuration source
     */
    public static DynamicPropertyFactory initWithConfigurationSource(AbstractConfiguration config) {
        synchronized (ConfigurationManager.class) {
            if (config == null) {
                throw new NullPointerException("config is null");
            }
            if (ConfigurationManager.isConfigurationInstalled() && config != ConfigurationManager.instance) {
                throw new IllegalStateException("ConfigurationManager is already initialized with configuration "
                        + ConfigurationManager.getConfigInstance());
            }
            if (config instanceof DynamicPropertySupport) {
                return initWithConfigurationSource((DynamicPropertySupport) config);
            }
            return initWithConfigurationSource(new ConfigurationBackedDynamicPropertySupportImpl(config));
        }
    }

    /**
     * Return whether the factory is initialized with the default ConcurrentCompositeConfiguration.
     */
    public static boolean isInitializedWithDefaultConfig() {
        return initializedWithDefaultConfig;
    }

    /**
     * Get the backing configuration from the factory. This can be cased to a {@link ConcurrentCompositeConfiguration}
     * if the default configuration is installed.
     * <p>For example:
     * <pre>
     *     Configuration config = DynamicPropertyFactory.getInstance().getBackingConfigurationSource();
     *     if (DynamicPropertyFactory.isInitializedWithDefaultConfig()) {
     *         ConcurrentCompositeConfiguration composite = (ConcurrentCompositeConfiguration) config;
     *         // ...
     *     }
     * </pre>
     *
     * @return the configuration source that DynamicPropertyFactory is initialized with. This will be an instance of
     *         Configuration, or {@link DynamicPropertySupport}, or null if DynamicPropertyFactory has not been initialized.
     */
    public static Object getBackingConfigurationSource() {
        if (config instanceof ConfigurationBackedDynamicPropertySupportImpl) {
            return ((ConfigurationBackedDynamicPropertySupportImpl) config).getConfiguration();
        } else {
            return config;
        }
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
     *         {@link MissingConfigurationSourceException} if there is no proper configuration source
     *         at the time of call.
     */
    public static boolean isThrowMissingConfigurationSourceException() {
        return throwMissingConfigurationSourceException;
    }

    /**
     * Initialize the factory with a {@link DynamicPropertySupport}.
     * <p/>
     * The initialization will register a {@link PropertyListener} with the DynamicPropertySupport so that DynamicProperty
     * will receives a callback and refresh its value when a property is changed.
     * <p/>
     * If the factory is already initialized with a default configuration source (see {@link #getInstance()}), it will re-register
     * itself with the new configuration source passed to this method. Otherwise, this method will throw IllegalArgumentException
     * if it has been initialized with a non-default configuration source. This method should be only called once.
     *
     * @param dynamicPropertySupport DynamicPropertySupport to be associated with the DynamicProperty
     * @return the instance of DynamicPropertyFactory
     * @throws IllegalArgumentException if the factory has already been initialized with a different and non-default configuration source
     */
    public static DynamicPropertyFactory initWithConfigurationSource(DynamicPropertySupport dynamicPropertySupport) {
        synchronized (ConfigurationManager.class) {
            if (dynamicPropertySupport == null) {
                throw new IllegalArgumentException("dynamicPropertySupport is null");
            }
            AbstractConfiguration configuration = null;
            if (dynamicPropertySupport instanceof AbstractConfiguration) {
                configuration = (AbstractConfiguration) dynamicPropertySupport;
            } else if (dynamicPropertySupport instanceof ConfigurationBackedDynamicPropertySupportImpl) {
                configuration = ((ConfigurationBackedDynamicPropertySupportImpl) dynamicPropertySupport).getConfiguration();
            }
            if (initializedWithDefaultConfig) {
                config = null;
            } else if (config != null && config != dynamicPropertySupport) {
                throw new IllegalStateException("DynamicPropertyFactory is already initialized with a diffrerent configuration source: " + config);
            }
            if (ConfigurationManager.isConfigurationInstalled()
                    && (configuration != null && configuration != ConfigurationManager.instance)) {
                throw new IllegalStateException("ConfigurationManager is already initialized with configuration "
                        + ConfigurationManager.getConfigInstance());
            }
            if (configuration != null && configuration != ConfigurationManager.instance) {
                ConfigurationManager.setDirect(configuration);
            }
            setDirect(dynamicPropertySupport);
            return instance;
        }
    }

    static void setDirect(DynamicPropertySupport support) {
        synchronized (ConfigurationManager.class) {
            config = support;
            DynamicProperty.registerWithDynamicPropertySupport(support);
            initializedWithDefaultConfig = false;
        }
    }

    /**
     * Get the instance to create dynamic properties. If the factory is not initialized with a configuration source
     * (see {@link #initWithConfigurationSource(AbstractConfiguration)} and {@link #initWithConfigurationSource(DynamicPropertySupport)}),
     * it will fist try to initialize itself with a default {@link ConcurrentCompositeConfiguration}, with the following two
     * sub configurations:
     * <ul>
     * <li>A SystemConfiguration, which contains all the system properties. You can disable adding this Configuration by setting
     * system property {@value #DISABLE_DEFAULT_SYS_CONFIG} to <code>true</code>
     * <li>A  {@link DynamicURLConfiguration}, which at a fixed interval polls
     * a configuration file (see {@link URLConfigurationSource#DEFAULT_CONFIG_FILE_NAME}) on classpath and a set of URLs specified via a system property
     * (see {@link URLConfigurationSource#CONFIG_URL}).
     * </ul>
     * Between the two sub-configurations, the SystemConfiguration will take the precedence when determining property values.
     * <p/>
     * You can disable the initialization with the default configuration by setting system property {@value #DISABLE_DEFAULT_CONFIG} to "true".
     */
    public static DynamicPropertyFactory getInstance() {
        if (config == null) {
            synchronized (ConfigurationManager.class) {
                if (config == null) {
                    AbstractConfiguration configFromManager = ConfigurationManager.getConfigInstance();
                    if (configFromManager != null) {
                        initWithConfigurationSource(configFromManager);
                        initializedWithDefaultConfig = !ConfigurationManager.isConfigurationInstalled();
                        logger.info("DynamicPropertyFactory is initialized with configuration sources: " + configFromManager);
                    }
                }
            }
        }
        return instance;
    }

    private static void checkAndWarn(String propName) {
        if (config == null) {
            logger.warn("DynamicProperty " + propName + " is created without a configuration source for callback.");
        }
    }

    /**
     * Create a new property whose value is a string and subject to change on-the-fly.
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicStringProperty getStringProperty(String propName, String defaultValue) {
        return getStringProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a string and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicStringProperty getStringProperty(String propName, String defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicStringProperty property = new DynamicStringProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is an integer and subject to change on-the-fly..
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicIntProperty getIntProperty(String propName, int defaultValue) {
        return getIntProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is an integer and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicIntProperty getIntProperty(String propName, int defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicIntProperty property = new DynamicIntProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a long and subject to change on-the-fly..
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicLongProperty getLongProperty(String propName, long defaultValue) {
        return getLongProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a long and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicLongProperty getLongProperty(String propName, long defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicLongProperty property = new DynamicLongProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a boolean and subject to change on-the-fly..
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicBooleanProperty getBooleanProperty(String propName, boolean defaultValue) {
        return getBooleanProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a boolean and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicBooleanProperty getBooleanProperty(String propName, boolean defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicBooleanProperty property = new DynamicBooleanProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a float and subject to change on-the-fly..
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicFloatProperty getFloatProperty(String propName, float defaultValue) {
        return getFloatProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a float and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicFloatProperty getFloatProperty(String propName, float defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicFloatProperty property = new DynamicFloatProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new property whose value is a double and subject to change on-the-fly..
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     */
    public DynamicDoubleProperty getDoubleProperty(String propName, double defaultValue) {
        return getDoubleProperty(propName, defaultValue, null);
    }

    /**
     * Create a new property whose value is a double and subject to change on-the-fly.
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     */
    public DynamicDoubleProperty getDoubleProperty(String propName, double defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicDoubleProperty property = new DynamicDoubleProperty(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    /**
     * Create a new contextual property of type T
     *
     * @param propName     property name
     * @param defaultValue default value if the property is not defined in underlying configuration
     * @param <T>          the type of the property value
     */
    public <T> DynamicContextualProperty<T> getContextualProperty(String propName, T defaultValue) {
        return getContextualProperty(propName, defaultValue, null);
    }

    /**
     * Create a new contextual property of type T
     *
     * @param propName               property name
     * @param defaultValue           default value if the property is not defined in underlying configuration
     * @param propertyChangeCallback a Runnable to be called when the property is changed
     * @param <T>                    the type of the property value
     */
    public <T> DynamicContextualProperty<T> getContextualProperty(String propName, T defaultValue, final Runnable propertyChangeCallback) {
        checkAndWarn(propName);
        DynamicContextualProperty<T> property = new DynamicContextualProperty<T>(propName, defaultValue);
        addCallback(propertyChangeCallback, property);
        return property;
    }

    private static void addCallback(Runnable callback, PropertyWrapper<?> wrapper) {
        if (callback != null) {
            wrapper.prop.addCallback(callback);
        }
    }

}
