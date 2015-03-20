package com.netflix.archaius.persisted2;

import java.util.Map;
import java.util.Set;

/**
 * Predicate for excluding properties that are no in scope
 * 
 * @author elandau
 *
 */
public interface ScopePredicate {
    public boolean evaluate(Map<String, Set<String>> attrs);
}
