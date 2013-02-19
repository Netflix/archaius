package com.netflix.config.sources;

import com.netflix.config.*;
import org.apache.commons.configuration.AbstractConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbDeploymentContextConfigurationSource implements PolledConfigurationSource {
    private final DynamoDbDeploymentContextTableCache tableCache;
    private final DeploymentContext.ContextKey contextKey;
    private final AbstractConfiguration cfg = ConfigurationManager.getConfigInstance();

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