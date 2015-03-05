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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implemenatation of {@link DeploymentContext} with simple setters and getters.
 * 
 * @author awang
 *
 */
public class SimpleDeploymentContext implements DeploymentContext {
    private Map<DeploymentContext.ContextKey, String> map = new ConcurrentHashMap<DeploymentContext.ContextKey, String>();
    
    @Override
    public String getDeploymentEnvironment() {
        return map.get(ContextKey.environment);
    }

    @Override
    public void setDeploymentEnvironment(String env) {
        map.put(ContextKey.environment, env);       
    }

    @Override
    public String getDeploymentDatacenter() {
        return map.get(ContextKey.datacenter);
    }

    @Override
    public void setDeploymentDatacenter(String deployedAt) {
        map.put(ContextKey.datacenter, deployedAt);
    }

    @Override
    public String getApplicationId() {
        return map.get(ContextKey.appId);
    }

    @Override
    public void setApplicationId(String appId) {
        map.put(ContextKey.appId, appId);
    }

    @Override
    public void setDeploymentServerId(String serverId) {
        map.put(ContextKey.serverId, serverId);
    }

    @Override
    public String getDeploymentServerId() {
        return map.get(ContextKey.serverId);
    }

    @Override
    public String getDeploymentStack() {
        return map.get(ContextKey.stack);
    }

    @Override
    public void setDeploymentStack(String stack) {
        map.put(ContextKey.stack, stack);
    }

    @Override
    public String getDeploymentRegion() {
        return map.get(ContextKey.region);
    }    

    @Override
    public void setDeploymentRegion(String region) {
        map.put(ContextKey.region, region);
    }

    @Override
    public String getValue(ContextKey key) {
        return map.get(key);
    }

    @Override
    public void setValue(ContextKey key, String value) {
        map.put(key, value);
    }    
}
