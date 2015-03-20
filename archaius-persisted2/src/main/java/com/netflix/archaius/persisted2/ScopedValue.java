package com.netflix.archaius.persisted2;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Encapsulate a single property value and its scopes. 
 * 
 * @author elandau
 */
public class ScopedValue {
    private final String value;
    private final LinkedHashMap<String, Set<String>> scopes;
    
    public ScopedValue(String value, LinkedHashMap<String, Set<String>> scopes) {
        this.value  = value;
        this.scopes = scopes;
    }

    public String getValue() {
        return value;
    }
    
    public LinkedHashMap<String, Set<String>> getScopes() {
        return scopes;
    }
}
