package com.netflix.archaius;

import java.util.List;

/**
 * Strategy for determining a set of cascading resource names.  The strategy will resolve
 * a single resource name into an ordered list of alternative names.
 * 
 * For example, a strategy may specify that additional configuration files may be loaded
 * based on environment and datacenter.  The strategy will return the list,
 * 
 * basename
 * basename-${environment}
 * basename-${datacenter}
 * basename-$[environment}-${datacenter}
 * 
 * @author elandau
 */
public interface CascadeStrategy {
    /**
     * Resolve a resource name to multiple alternative names.
     * 
     * @param interpolator Interpolator for variable replacements
     * @param resource The resource name
     * 
     * @return List of all names including the original name
     */
    List<String> generate(String resource, StrInterpolator interpolator);
}
