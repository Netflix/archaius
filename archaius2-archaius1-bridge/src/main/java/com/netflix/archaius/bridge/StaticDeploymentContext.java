package com.netflix.archaius.bridge;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.config.DeploymentContext;

/**
 * @see StaticArchaiusBridgeModule
 * @author elandau
 */
@Singleton
public class StaticDeploymentContext implements DeploymentContext {
    private static volatile DeploymentContext delegate;
    
    @Inject
    public static void initialize(DeploymentContext context) {
        delegate = context;
    }
    
    public static void reset() {
        delegate = null;
    }
    
    @Override
    public String getDeploymentEnvironment() {
        return getValue(ContextKey.environment);
    }

    @Override
    public void setDeploymentEnvironment(String env) {
        delegate.setDeploymentEnvironment(env);
    }

    @Override
    public String getDeploymentDatacenter() {
        return getValue(ContextKey.datacenter);
    }

    @Override
    public void setDeploymentDatacenter(String deployedAt) {
        delegate.setDeploymentDatacenter(deployedAt);
    }

    @Override
    public String getApplicationId() {
        return getValue(ContextKey.appId);
    }

    @Override
    public void setApplicationId(String appId) {
        delegate.setApplicationId(appId);
    }

    @Override
    public String getDeploymentServerId() {
        return getValue(ContextKey.serverId);
    }

    @Override
    public void setDeploymentServerId(String serverId) {
        delegate.setDeploymentServerId(serverId);
    }

    @Override
    public String getDeploymentStack() {
        return getValue(ContextKey.stack);
    }

    @Override
    public void setDeploymentStack(String stack) {
        delegate.setDeploymentStack(stack);
    }

    @Override
    public String getDeploymentRegion() {
        return getValue(ContextKey.region);
    }

    @Override
    public void setDeploymentRegion(String region) {
        delegate.setDeploymentRegion(region);
    }

    @Override
    public String getValue(ContextKey key) {
        if (delegate == null) {
            System.out.println("Configuration not yet initialized.  Returning 'null' for " + key);
            return null;
        }
        
        return delegate.getValue(key);
    }

    @Override
    public void setValue(ContextKey key, String value) {
        delegate.setValue(key, value);
    }

}
