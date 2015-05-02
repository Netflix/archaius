package com.netflix.archaius.persisted2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultPersisted2ClientConfig implements Persisted2ClientConfig {

    private int refreshRate = 30;
    private List<String> prioritizedScopes = new ArrayList<>();
    private Map<String, List<String>> queryScopes = new HashMap<>();
    private String serviceUrl;
    private Map<String, String> scopes = new HashMap<>();
    private boolean skipPropsWithExtraScopes = false;
    
    public DefaultPersisted2ClientConfig withRefreshRate(int refreshRate) {
        this.refreshRate = refreshRate;
        return this;
    }
    
    @Override
    public int getRefreshRate() {
        return refreshRate;
    }

    public DefaultPersisted2ClientConfig withPrioritizedScopes(List<String> scopes) {
        this.prioritizedScopes = scopes;
        return this;
    }
    
    public DefaultPersisted2ClientConfig withPrioritizedScopes(String ... scopes) {
        this.prioritizedScopes = Arrays.asList(scopes);
        return this;
    }
    
    @Override
    public List<String> getPrioritizedScopes() {
        return this.prioritizedScopes;
    }

    public DefaultPersisted2ClientConfig withScope(String name, String value) {
        this.scopes.put(name, value);
        return this;
    }
    
    @Override
    public Map<String, String> getScopes() {
        return scopes;
    }

    public DefaultPersisted2ClientConfig withQueryScope(String name, String ... values) {
        queryScopes.put(name, Arrays.asList(values));
        return this;
    }
    
    @Override
    public Map<String, List<String>> getQueryScopes() {
        return queryScopes;
    }

    public DefaultPersisted2ClientConfig withServiceUrl(String url) {
        this.serviceUrl = url;
        return this;
    }
    
    @Override
    public String getServiceUrl() {
        return this.serviceUrl;
    }

    public DefaultPersisted2ClientConfig withSkipPropsWithExtraScopes(boolean value) {
        this.skipPropsWithExtraScopes = value;
        return this;
    }
    @Override
    public boolean getSkipPropsWithExtraScopes() {
        return skipPropsWithExtraScopes;
    }

}
