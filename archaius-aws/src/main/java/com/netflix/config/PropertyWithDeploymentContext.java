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
    private final String propertyValue;

    public PropertyWithDeploymentContext(DeploymentContext.ContextKey contextKey, String contextValue, String propertyName, String propertyValue) {
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

    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyWithDeploymentContext that = (PropertyWithDeploymentContext) o;

        if (contextKey != that.contextKey) return false;
        if (!contextValue.equals(that.contextValue)) return false;
        if (!propertyName.equals(that.propertyName)) return false;
        if (!propertyValue.equals(that.propertyValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = contextKey.hashCode();
        result = 31 * result + contextValue.hashCode();
        result = 31 * result + propertyName.hashCode();
        result = 31 * result + propertyValue.hashCode();
        return result;
    }
}
