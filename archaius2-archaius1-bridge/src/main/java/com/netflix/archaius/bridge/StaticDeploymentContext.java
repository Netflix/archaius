package com.netflix.archaius.bridge;

import javax.inject.Inject;

import com.netflix.config.DeploymentContext;

/**
 * @see StaticArchaiusBridgeModule
 * @author elandau
 */
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
        return delegate.getDeploymentEnvironment();
    }

    @Override
    public void setDeploymentEnvironment(String env) {
        delegate.setDeploymentEnvironment(env);
    }

    @Override
    public String getDeploymentDatacenter() {
        return delegate.getDeploymentDatacenter();
    }

    @Override
    public void setDeploymentDatacenter(String deployedAt) {
        delegate.setDeploymentDatacenter(deployedAt);
    }

    @Override
    public String getApplicationId() {
        return delegate.getApplicationId();
    }

    @Override
    public void setApplicationId(String appId) {
        delegate.setApplicationId(appId);
    }

    @Override
    public void setDeploymentServerId(String serverId) {
        delegate.setDeploymentServerId(serverId);
    }

    @Override
    public String getDeploymentServerId() {
        return delegate.getDeploymentServerId();
    }

    @Override
    public String getDeploymentStack() {
        return delegate.getDeploymentStack();
    }

    @Override
    public String getValue(ContextKey key) {
        return delegate.getValue(key);
    }

    @Override
    public void setValue(ContextKey key, String value) {
        delegate.setValue(key, value);
    }

    @Override
    public void setDeploymentStack(String stack) {
        delegate.setDeploymentStack(stack);
    }

    @Override
    public String getDeploymentRegion() {
        return delegate.getDeploymentRegion();
    }

    @Override
    public void setDeploymentRegion(String region) {
        delegate.setDeploymentRegion(region);
    }
}
