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
