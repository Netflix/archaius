package com.netflix.config;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * Wrapper object to represent a property that also has a deployment context.
 */
public class PropertyWithDeploymentContext {
    private final DeploymentContext.ContextKey contextKey;
    private final String contextValue;
    private final String propertyName;
    private final Object propertyValue;

    public PropertyWithDeploymentContext(DeploymentContext.ContextKey contextKey, String contextValue, String propertyName, Object propertyValue) {
        this.contextKey = contextKey;
        this.contextValue = contextValue;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public DeploymentContext.ContextKey getContextKey() {
        return contextKey;
    }

    public String getContextValue() {
        return contextValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }
}
