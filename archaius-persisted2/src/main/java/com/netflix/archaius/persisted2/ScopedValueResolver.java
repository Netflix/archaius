package com.netflix.archaius.persisted2;

import java.util.List;

/**
 * Contract for resolving a list of ScopesValues into a single value.
 * 
 * @author elandau
 *
 */
public interface ScopedValueResolver {
    String resolve(String propName, List<ScopedValue> variations);
}
