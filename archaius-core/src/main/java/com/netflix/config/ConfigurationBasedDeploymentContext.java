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

import org.apache.commons.configuration.Configuration;

public class ConfigurationBasedDeploymentContext extends SimpleDeploymentContext {
    
    @Override
    public String getDeploymentEnvironment() {
        String value = getValueFromConfig("archaius.deployment.environment");
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentEnvironment();
        }
    }

    @Override
    public String getDeploymentDatacenter() {
        String value = getValueFromConfig("archaius.deployment.datacenter");
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentDatacenter();
        }
    }

    @Override
    public String getApplicationId() {
        String value = getValueFromConfig("archaius.deployment.applicationId");
        if (value != null) {
            return value;
        } else {
            return super.getApplicationId();
        }
    }

    @Override
    public String getDeploymentServerId() {
        String value = getValueFromConfig("archaius.deployment.serverId");
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentServerId();
        }
    }

    @Override
    public String getDeploymentStack() {
        String value = getValueFromConfig("archaius.deployment.stack");
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentStack();
        }
    }

    @Override
    public String getDeploymentRegion() {
        String value = getValueFromConfig("archaius.deployment.region");
        if (value != null) {
            return value;
        } else {
            return super.getDeploymentRegion();
        }
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

}
