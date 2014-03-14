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
package com.netflix.config.sources;

import com.netflix.config.*;
import org.apache.commons.configuration.AbstractConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * More advanced Dynamo source that allows you to filter the results based on the current deployment context values.
 * Rather than polling dynamo itself this class reads a cache that you have to setup separately.  If you used a combined
 * configuration you can cascade these sources to so that certain contexts override others.
 */
public class DynamoDbDeploymentContextConfigurationSource implements PolledConfigurationSource {
    private final DynamoDbDeploymentContextTableCache tableCache;
    private final DeploymentContext.ContextKey contextKey;
    private final DeploymentContext deploymentContext = ConfigurationManager.getDeploymentContext();


    /**
     * The configuration will be filtered based on the contextKey.
     * @param tableCache
     * @param contextKey - Null can be used as a wild card to match the absence of a context key for the property.
     *                   The result would be that this source with a null key would pick up "global" properties.
     */
    public DynamoDbDeploymentContextConfigurationSource(DynamoDbDeploymentContextTableCache tableCache, DeploymentContext.ContextKey contextKey) {
        this.tableCache = tableCache;
        this.contextKey = contextKey;
    }

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        for(PropertyWithDeploymentContext prop: tableCache.getProperties()){
            if(prop.getContextKey() == contextKey && prop.getContextValue() == null){
                map.put(prop.getPropertyName(), prop.getPropertyValue());
            }
            else if(prop.getContextKey() == contextKey &&
                    prop.getContextValue().equalsIgnoreCase(deploymentContext.getValue(contextKey))){
                map.put(prop.getPropertyName(), prop.getPropertyValue());
            }
        }

        return PollResult.createFull(map);
    }
}