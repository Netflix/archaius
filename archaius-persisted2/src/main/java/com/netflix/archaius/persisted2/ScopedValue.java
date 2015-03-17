package com.netflix.archaius.persisted2;

import java.util.LinkedHashMap;

/**
 * Encapsulate a single property value and its scopes. 
 * 
 * @author elandau
 */
public class ScopedValue {
    private final String value;
    private final LinkedHashMap<String, String> scopes;
    
    public ScopedValue(String value, LinkedHashMap<String, String> scopes) {
        this.value  = value;
        this.scopes = scopes;
    }

    public String getValue() {
        return value;
    }
    
    public LinkedHashMap<String, String> getScopes() {
        return scopes;
    }
}
