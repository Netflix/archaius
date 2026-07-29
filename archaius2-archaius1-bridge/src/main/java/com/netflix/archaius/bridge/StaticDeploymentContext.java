package com.netflix.archaius.bridge;

import com.netflix.config.DeploymentContext;

import jakarta.inject.Inject;

public final class StaticDeploymentContext implements DeploymentContext {
    private static final StaticDeploymentContext INSTANCE = new StaticDeploymentContext();
    
    private static volatile DeploymentContext delegate;
    
    @Inject
    public static void initialize(DeploymentContext context) {
        delegate = context;
    }
    
    public static void reset() {
        delegate = null;
    }
    
    public static DeploymentContext getInstance() {
        return INSTANCE;
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
        setValue(ContextKey.appId, appId);
    }
    
    @Override
    public String getDeploymentServerId() {
        return getValue(ContextKey.serverId);
    }

    @Override
    public void setDeploymentServerId(String serverId) {
        setValue(ContextKey.serverId, serverId);
    }

    @Override
    public String getDeploymentStack() {
        return getValue(ContextKey.stack);
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

    @Override
    public void setDeploymentStack(String stack) {
        setValue(ContextKey.stack, stack);
    }

    @Override
    public String getDeploymentRegion() {
        return getValue(ContextKey.region);
    }

    @Override
    public void setDeploymentRegion(String region) {
        setValue(ContextKey.region, region);
    }

}
