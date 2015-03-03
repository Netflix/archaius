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

import netflix.archaius.Config;

/**
 * An implementation of {@link SimpleDeploymentContext} based on instance of
 * {@link Config}. 
 *
 * @author elandau
 */
public class ConfigurationBasedDeploymentContext extends SimpleDeploymentContext {

    private final Config config;
    
    public ConfigurationBasedDeploymentContext(Config config) {
        this.config = config;
    }

    @Override
    public String getDeploymentEnvironment() {
        return getValue(ContextKey.environment);
    }

    @Override
    public String getDeploymentDatacenter() {
        return getValue(ContextKey.datacenter);
    }

    @Override
    public String getApplicationId() {
        return getValue(ContextKey.appId);
    }

    @Override
    public String getDeploymentServerId() {
        return getValue(ContextKey.serverId);
    }

    @Override
    public String getDeploymentStack() {
        return getValue(ContextKey.stack);
    }

    @Override
    public String getDeploymentRegion() {
        return getValue(ContextKey.region);
    }
    
    @Override
    public String getValue(ContextKey key) {
        return config.getString(ContextKey.environment.getKey(), super.getValue(key));
    }
}
