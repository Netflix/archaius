package com.netflix.archaius.persisted2;

import java.util.HashMap;
import java.util.Map;

import com.netflix.archaius.Config;

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
            public boolean evaluate(Map<String, String> attrs) {
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
                    lookup.put(key, value);
                }
                return value;
            }
        };
    }
    
    public static ScopePredicate fromMap(final Map<String, String> values) {
        return new AbstractScopePredicate() {
            @Override
            public String getScope(String key) {
                String value = values.get(key);
                return value == null ? "" : value;
            }
        };
    }
}
