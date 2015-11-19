package com.netflix.archaius.persisted2;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.netflix.archaius.api.Config;

/**
 * Utility class for creating common ScopePredicates
 * 
 * @author elandau
 *
 */
public abstract class ScopePredicates {
    public static ScopePredicate alwaysTrue() {
        return new ScopePredicate() {
            @Override
            public boolean evaluate(Map<String, Set<String>> attrs) {
                return true;
            }
        };
    }
    
    public static ScopePredicate fromConfig(final Config config) {
        final HashMap<String, String> lookup = new HashMap<String, String>();
        return new AbstractScopePredicate() {
            @Override
            public String getScope(String key) {
                String value = lookup.get(key);
                if (value == null) {
                    value = config.getString(key, "");
                    lookup.put(key, value.toLowerCase());
                }
                return value;
            }
        };
    }
    
    public static ScopePredicate fromMap(final Map<String, String> values) {
        final Map<String, String> lowerCaseValues = new HashMap<String, String>();
        for (Entry<String, String> entry : values.entrySet()) {
            lowerCaseValues.put(entry.getKey(), entry.getValue().toLowerCase());
        }
        return new AbstractScopePredicate() {
            @Override
            public String getScope(String key) {
                String value = lowerCaseValues.get(key);
                return value == null ? "" : value;
            }
        };
    }
}
