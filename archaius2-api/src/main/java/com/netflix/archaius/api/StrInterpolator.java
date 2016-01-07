/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.api;

/**
 * API for interpolating a string.  
 * 
 * For example,
 * 
 * <pre>
 * foo=abc
 * {@code
 *  interpolator.create(lookup).resolve("123-${foo}") -> 123-abc
 * }
 * </pre>
 * 
 * @author elandau
 *
 */
public interface StrInterpolator {
    /**
     * Lookup of a raw string for replacements.  The lookup should not do any replacements.
     * If a string with replacements is returned the interpolator will extract the key and
     * call back into the lookup to get the value for that key.
     * 
     * @author elandau
     */
    public interface Lookup {
        String lookup(String key);
    }

    /**
     * Top level context 
     * @author elandau
     *
     */
    public interface Context {
        /**
         * Resolve a string with replaceable variables using the provided map to lookup replacement
         * values.  The implementation should deal with nested replacements and throw an exception
         * for infinite recursion. 
         * 
         * @param value
         * @return
         */
        String resolve(String value);
    }
    
    /**
     * Create a context though which a value may be resolved.  A different context should be created
     * for each string being resolved since it tracks state to handle things like circular references.
     * 
     * <pre>
     * {@code
     *    interpolator.create(lookup).resolve("prefix-${foo}");
     * }
     * </pre>
     * @author elandau
     *
     */
    Context create(Lookup lookup);
}
