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
package com.netflix.archaius.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.StrInterpolator;

/**
 * Given a list of parameters generate all combinations by progressively
 * concatinating the next parameter
 * 
 * ${name}-${param1}
 * ${name}-${param1}-${param2}
 * ${name}-${param1}-${param2}-${param3}
 * 
 * @author elandau
 *
 */
public class ConcatCascadeStrategy implements CascadeStrategy {
    private static final String DEFAULT_SEPARATOR = "-";
    
    private final List<String> parameters;
    private final String separator;
    
    public static ConcatCascadeStrategy from(String ... parameters) {
        ArrayList<String> params = new ArrayList<String>();
        Collections.addAll(params, parameters);
        return new ConcatCascadeStrategy(params);
    }
    
    public ConcatCascadeStrategy(List<String> parameters) {
        this.separator = DEFAULT_SEPARATOR;
        this.parameters = new ArrayList<String>();
        this.parameters.addAll(parameters);
    }
    
    public ConcatCascadeStrategy(String[] parameters) {
        this(Arrays.asList(parameters));
    }
    
    public ConcatCascadeStrategy(String separator, List<String> parameters) {
        this.separator = separator;
        this.parameters = new ArrayList<String>();
        this.parameters.addAll(parameters);
    }
    
    public ConcatCascadeStrategy(String separator, String[] parameters) {
        this(separator, Arrays.asList(parameters));
    }
    
    @Override
    public List<String> generate(String name, StrInterpolator interpolator, StrInterpolator.Lookup lookup) {
        ArrayList<String> result = new ArrayList<String>();
        
        result.add(name);
        
        String current = name;
        for (String param : parameters) {
            current += separator + param;
            result.add(interpolator.create(lookup).resolve(current));
        }

        return result;
    }

}
