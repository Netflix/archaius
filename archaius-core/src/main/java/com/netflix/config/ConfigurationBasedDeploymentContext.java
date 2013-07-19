/**
 * Copyright 2013 Netflix, Inc.
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
import org.apache.commons.configuration.Configuration;

/**
 * An implementation of {@link DeploymentContext} based on system wide configuration set with
 * {@link ConfigurationManager}. All the getters will first consult corresponding property
 * and return the value if set.
 *
 * @author awang
 */
public class ConfigurationBasedDeploymentContext extends SimpleDeploymentContext {

    public static final String DEPLOYMENT_ENVIRONMENT_PROPERTY = "archaius.deployment.environment";
    public static final String DEPLOYMENT_DATACENTER_PROPERTY = "archaius.deployment.datacenter";
    public static final String DEPLOYMENT_APPLICATION_ID_PROPERTY = "archaius.deployment.applicationId";
    public static final String DEPLOYMENT_SERVER_ID_PROPERTY = "archaius.deployment.serverId";
    public static final String DEPLOYMENT_STACK_PROPERTY = "archaius.deployment.stack";
    public static final String DEPLOYMENT_REGION_PROPERTY = "archaius.deployment.region";

    /**
     * Get the deployment environment. If property "archaius.deployment.environment"
     * is set in the system wide configuration, it will return it. Otherwise, it will return super.getDeploymentEnvironment().
     */
    @Override
    public String getDeploymentEnvironment() {
        String value = getValueFromConfig(DEPLOYMENT_ENVIRONMENT_PROPERTY);
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentEnvironment();
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
        String value = getValueFromConfig(DEPLOYMENT_DATACENTER_PROPERTY);
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
        String value = getValueFromConfig(DEPLOYMENT_APPLICATION_ID_PROPERTY);
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
        String value = getValueFromConfig(DEPLOYMENT_SERVER_ID_PROPERTY);
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
        String value = getValueFromConfig(DEPLOYMENT_STACK_PROPERTY);
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
        String value = getValueFromConfig(DEPLOYMENT_REGION_PROPERTY);
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
        String value = getValueFromConfig("archaius.deployment." + key.toString());
        if (value != null) {
            return value;
        } else {
            return super.getValue(key);
        }
    }

}
