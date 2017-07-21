package com.netflix.archaius.bridge;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.config.DeploymentContext;

/**
 * DeploymentContext that reads the ContextKey properties from the top level
 * injected Config
 * 
 * @author elandau
 */
@Singleton
public class ConfigBasedDeploymentContext implements DeploymentContext {

    private static DeploymentContext staticContext;
    
    private final Config config;
    private final SettableConfig override;
    
    @Inject
    public static void initialize(DeploymentContext context) {
        staticContext = context;
    }
    
    public static void reset() {
        staticContext = null;
    }

    public static DeploymentContext getInstance() {
        if (staticContext == null) {
            throw new RuntimeException("DeploymentContext not initialized yet.");
        }
        return staticContext;
    }
    
    @Inject
    public ConfigBasedDeploymentContext(Config config, @RuntimeLayer SettableConfig override) {
        this.config = config;
        this.override = override;
    }
    
    @Override
    public String getDeploymentEnvironment() {
        return config.getString(ContextKey.environment.getKey(), "");
    }

    @Override
    public void setDeploymentEnvironment(String env) {
        override.setProperty(ContextKey.environment.getKey(), env);
    }

    @Override
    public String getDeploymentDatacenter() {
        return config.getString(ContextKey.datacenter.getKey(), "");
    }

    @Override
    public void setDeploymentDatacenter(String deployedAt) {
        override.setProperty(ContextKey.datacenter.getKey(), deployedAt);
    }

    @Override
    public String getApplicationId() {
        return config.getString(ContextKey.appId.getKey(), "");
    }

    @Override
    public void setApplicationId(String appId) {
        override.setProperty(ContextKey.appId.getKey(), appId);
    }

    @Override
    public void setDeploymentServerId(String serverId) {
        override.setProperty(ContextKey.serverId.getKey(), serverId);
    }

    @Override
    public String getDeploymentServerId() {
        return config.getString(ContextKey.serverId.getKey(), "");
    }

    @Override
    public String getDeploymentStack() {
        return config.getString(ContextKey.stack.getKey(), "");
    }

    @Override
    public String getValue(ContextKey key) {
        return config.getString(key.getKey(), "");
    }

    @Override
    public void setValue(ContextKey key, String value) {
        override.setProperty(key.getKey(), value);
    }

    @Override
    public void setDeploymentStack(String stack) {
        override.setProperty(ContextKey.stack.getKey(), stack);
    }

    @Override
    public String getDeploymentRegion() {
        return config.getString(ContextKey.region.getKey(), "");
    }

    @Override
    public void setDeploymentRegion(String region) {
        override.setProperty(ContextKey.region.getKey(), region);
    }
}
