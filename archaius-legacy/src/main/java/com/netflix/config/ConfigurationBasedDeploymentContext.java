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

import com.google.common.base.Preconditions;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;

/**
 * An implementation of {@link DeploymentContext} based on system wide configuration set with
 * {@link ConfigurationManager}. All the getters will first consult corresponding property
 * and return the value if set.
 *
 * @author awang
 */
public class ConfigurationBasedDeploymentContext extends SimpleDeploymentContext {

    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#environment} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_ENVIRONMENT_PROPERTY = "archaius.deployment.environment";
    
    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#datacenter} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_DATACENTER_PROPERTY = "archaius.deployment.datacenter";
    
    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#appId} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_APPLICATION_ID_PROPERTY = "archaius.deployment.applicationId";
    
    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#serverId} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_SERVER_ID_PROPERTY = "archaius.deployment.serverId";
    
    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#stack} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_STACK_PROPERTY = "archaius.deployment.stack";
    
    /**
     * should use value from {@link ContextKey#getKey()} on {@link ContextKey#environment} as the property to set or get 
     */
    @Deprecated
    public static final String DEPLOYMENT_REGION_PROPERTY = "archaius.deployment.region";

    private ConfigurationListener configListener = new ConfigurationListener() {
        
        @Override
        public void configurationChanged(ConfigurationEvent event) {
            if (event.isBeforeUpdate() 
                    || (event.getType() != AbstractConfiguration.EVENT_ADD_PROPERTY
                            && event.getType() != AbstractConfiguration.EVENT_SET_PROPERTY)) {
                return;
            }
            String name = event.getPropertyName();
            String value = event.getPropertyValue() == null ? null : String.valueOf(event.getPropertyValue());
            if (value == null) {
                return;
            }
            if (name.equals(DEPLOYMENT_ENVIRONMENT_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setDeploymentRegion(value);
                setValueInConfig(ContextKey.environment.getKey(), value);                
            } else if (name.equals(DEPLOYMENT_DATACENTER_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setDeploymentDatacenter(value);
                setValueInConfig(ContextKey.datacenter.getKey(), value);                
            } else if (name.equals(DEPLOYMENT_STACK_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setDeploymentStack(value);
                setValueInConfig(ContextKey.stack.getKey(), value);                
            } else if (name.equals(DEPLOYMENT_APPLICATION_ID_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setApplicationId(value);
                setValueInConfig(ContextKey.appId.getKey(), value);                
            } else if (name.equals(DEPLOYMENT_REGION_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setDeploymentRegion(value);
                setValueInConfig(ContextKey.region.getKey(), value);                
            } else if (name.equals(DEPLOYMENT_SERVER_ID_PROPERTY)) {
                ConfigurationBasedDeploymentContext.super.setDeploymentServerId(value);
                setValueInConfig(ContextKey.serverId.getKey(), value);                
            }
        }
    };
    
    public ConfigurationBasedDeploymentContext() {
        super();
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        if (config != null) {
            String contextValue = getValueFromConfig(DEPLOYMENT_APPLICATION_ID_PROPERTY);
            if (contextValue != null) {
                setApplicationId(contextValue);
            }
            contextValue = getValueFromConfig(DEPLOYMENT_DATACENTER_PROPERTY);
            if (contextValue != null) {
                setDeploymentDatacenter(contextValue);
            }
            contextValue = getValueFromConfig(DEPLOYMENT_ENVIRONMENT_PROPERTY);
            if (contextValue != null) {
                setDeploymentEnvironment(contextValue);
            }
            contextValue = getValueFromConfig(DEPLOYMENT_REGION_PROPERTY);
            if (contextValue != null) {
                setDeploymentRegion(contextValue);
            }
            contextValue = getValueFromConfig(DEPLOYMENT_STACK_PROPERTY);
            if (contextValue != null) {
                setDeploymentStack(contextValue);
            }    
            contextValue = getValueFromConfig(DEPLOYMENT_SERVER_ID_PROPERTY);
            if (contextValue != null) {
                setDeploymentServerId(contextValue);
            }    
            config.addConfigurationListener(configListener);
        }
    }
    
    /**
     * Get the deployment environment. If property "archaius.deployment.environment"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentEnvironment().
     */
    @Override
    public String getDeploymentEnvironment() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.environment.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_ENVIRONMENT_PROPERTY);
            if (value != null) {
                return value;
            } else {
                return super.getDeploymentEnvironment();
            }
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setDeploymentEnvironment(String value) {
        super.setDeploymentEnvironment(value);
        setValueInConfig(DEPLOYMENT_ENVIRONMENT_PROPERTY, value);
        setValueInConfig(ContextKey.environment.getKey(), value);
    }

    /**
     * Get the deployment environment. If property "archaius.deployment.datacenter"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentDatacenter().
     */
    @Override
    public String getDeploymentDatacenter() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.datacenter.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_DATACENTER_PROPERTY);
        }
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentDatacenter();
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setDeploymentDatacenter(String value) {
        super.setDeploymentDatacenter(value);
        setValueInConfig(DEPLOYMENT_DATACENTER_PROPERTY, value);
        setValueInConfig(ContextKey.datacenter.getKey(), value);
    }

    /**
     * Get the deployment environment. If property "archaius.deployment.applicationId"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getApplicationId().
     */
    @Override
    public String getApplicationId() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.appId.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_APPLICATION_ID_PROPERTY);
        }
        if (value != null) {
            return value;
        } else {
            return super.getApplicationId();
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setApplicationId(String value) {
        super.setApplicationId(value);
        setValueInConfig(DEPLOYMENT_APPLICATION_ID_PROPERTY, value);
        setValueInConfig(ContextKey.appId.getKey(), value);
    }

    /**
     * Get the deployment environment. If property "archaius.deployment.serverId"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentServerId().
     */
    @Override
    public String getDeploymentServerId() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.serverId.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_SERVER_ID_PROPERTY);
        }
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentServerId();
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setDeploymentServerId(String value) {
        super.setDeploymentServerId(value);
        setValueInConfig(DEPLOYMENT_SERVER_ID_PROPERTY, value);
        setValueInConfig(ContextKey.serverId.getKey(), value);
    }

    /**
     * Get the deployment environment. If property "archaius.deployment.stack"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentStack().
     */
    @Override
    public String getDeploymentStack() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.stack.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_STACK_PROPERTY);
        }
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentStack();
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setDeploymentStack(String value) {
        super.setDeploymentStack(value);
        setValueInConfig(DEPLOYMENT_STACK_PROPERTY, value);
        setValueInConfig(ContextKey.stack.getKey(), value);
    }

    /**
     * Get the deployment environment. If property "archaius.deployment.region"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentRegion().
     */
    @Override
    public String getDeploymentRegion() {
        String value = getValueFromConfig(DeploymentContext.ContextKey.region.getKey());
        if (value != null) {
            return value;
        } else {
            value = getValueFromConfig(DEPLOYMENT_REGION_PROPERTY);
        }
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentRegion();
        }
    }

    /**
     * Call super and also update the configuration to reflect the changes.
     *
     * @param value
     */
    @Override
    public void setDeploymentRegion(String value) {
        super.setDeploymentRegion(value);
        setValueInConfig(DEPLOYMENT_REGION_PROPERTY, value);
        setValueInConfig(ContextKey.region.getKey(), value);
    }

    private String getValueFromConfig(String name) {
        Configuration conf = ConfigurationManager.getConfigInstance();
        if (conf == null) {
            return null;
        }
        String value = (String) conf.getProperty(name);
        if (value != null) {
            return value;
        } else {
            value = System.getProperty(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void setValueInConfig(String name, String value) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(value);
        ConfigurationManager.getConfigInstance().setProperty(name, value);
    }

    @Override
    public String getValue(ContextKey key) {
        String value = getValueFromConfig(key.getKey());
        if (value != null) {
            return value;
        } else {
            return super.getValue(key);
        }
    }

}
