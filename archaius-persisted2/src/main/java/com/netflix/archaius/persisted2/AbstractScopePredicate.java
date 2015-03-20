package com.netflix.archaius.persisted2;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class AbstractScopePredicate implements ScopePredicate {

    @Override
    public boolean evaluate(Map<String, Set<String>> scopes) {
        for (Entry<String, Set<String>> scope : scopes.entrySet()) {
            // TODO: split into list
            if (!scope.getValue().isEmpty() && 
                !scope.getValue().contains(getScope(scope.getKey()).toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getScope(String key);
}
