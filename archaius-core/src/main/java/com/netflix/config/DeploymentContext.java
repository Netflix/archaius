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

public interface DeploymentContext {

    public String getDeploymentEnvironment();

    public void setDeploymentEnvironment(String env);

    public String getDeploymentDatacenter();

    public void setDeploymentDatacenter(String deployedAt);

    public String getApplicationId();

    public void setApplicationId(String appId);

    public void setDeploymentServerId(String serverId);

    public String getDeploymentServerId();

    public String getDeploymentStack();

    public void setDeploymentStack(String stack);

    public String getDeploymentRegion();

    public void setDeploymentRegion(String region);    
}
