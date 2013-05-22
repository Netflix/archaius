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

/**
 * An interface to define the deployment context of an application. All attributes
 * are optional and may return null if unset.
 */
public interface DeploymentContext {
    
    public enum ContextKey {
        environment("@environment"), datacenter("@datacenter"), appId("@appId"),
        serverId("@serverId"), stack("@stack"), region("@region"), zone("@zone");
        
        private String key;
        
        ContextKey(String key) {
            this.key = key;
        }
        
        public String getKey() {
            return key;
        }
    }
    
    /**
     * @return the deployment environment. For example "test", "dev", "prod".
     */
    public String getDeploymentEnvironment();

    public void setDeploymentEnvironment(String env);

    /**
     * @return the name or ID of the data center.
     */
    public String getDeploymentDatacenter();

    public void setDeploymentDatacenter(String deployedAt);

    public String getApplicationId();

    public void setApplicationId(String appId);

    public void setDeploymentServerId(String serverId);

    public String getDeploymentServerId();

    /**
     * 
     * @return a vertical stack name where this application is deployed. The stack name
     * can be used to affect the application's behavior.
     */
    public String getDeploymentStack();
    
    public String getValue(ContextKey key);
    
    public void setValue(ContextKey key, String value);

    public void setDeploymentStack(String stack);

    /**
     * 
     * @return region of the deployment. In EC2, this could be 
     * Amazon region "us-east-1", "us-west-1", etc.
     */
    public String getDeploymentRegion();

    public void setDeploymentRegion(String region);    
}
