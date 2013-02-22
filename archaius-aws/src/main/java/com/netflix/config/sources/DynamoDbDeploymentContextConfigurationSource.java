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
    private final AbstractConfiguration cfg = ConfigurationManager.getConfigInstance();


    /**
     * The configuration will be filtered based on the contextKey.
     * @param tableCache
     * @param contextKey
     */
    public DynamoDbDeploymentContextConfigurationSource(DynamoDbDeploymentContextTableCache tableCache, DeploymentContext.ContextKey contextKey) {
        this.tableCache = tableCache;
        this.contextKey = contextKey;
    }

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        for(PropertyWithDeploymentContext prop: tableCache.getProperties()){
            if(prop.getContextKey() == contextKey &&
                    prop.getContextValue().equalsIgnoreCase(cfg.getString(prop.getContextKey().getKey()))){
                map.put(prop.getPropertyName(), prop.getPropertyValue());
            }
        }

        return PollResult.createFull(map);
    }
}