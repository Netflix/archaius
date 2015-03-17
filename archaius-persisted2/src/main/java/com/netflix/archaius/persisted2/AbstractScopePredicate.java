package com.netflix.archaius.persisted2;

import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractScopePredicate implements ScopePredicate {

    @Override
    public boolean evaluate(Map<String, String> attrs) {
        for (Entry<String, String> entry : attrs.entrySet()) {
            if (!getScope(entry.getKey()).equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getScope(String key);
}
