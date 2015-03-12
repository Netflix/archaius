package com.netflix.archaius;

/**
 * API for interpolating a string.  
 * 
 * For example,
 * 
 * foo=abc
 * 
 * resolve("123-$foo") -> 123-abc
 * 
 * @author elandau
 *
 */
public interface StrInterpolator {
    /**
     * Resolve a string with replaceable variables using the provided map to lookup replacement
     * values.  The implementation should deal with nested replacements and throw an exception
     * for infinite recursion. 
     * 
     * @param key
     * @param lookup
     * @return
     */
    String resolve(String key);
}
