/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.config;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * The default predicate (used by {@link DynamicContextualProperty}) which takes a Function to get the value of
 * key included in the JSON blob as the input string value of {@link DynamicContextualProperty}.
 *   
 * @author awang
 *
 */
public class DefaultContextualPredicate implements Predicate<Map<String, Collection<String>>> {

    private final Function<String, String> getValueFromKeyFunction;
    
    public static final DefaultContextualPredicate PROPERTY_BASED = new DefaultContextualPredicate(new Function<String, String>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public String apply(@Nullable String input) {
            return DynamicProperty.getInstance(input).getString(); 
        }
        
    });
    
    public DefaultContextualPredicate(Function<String, String> getValueFromKeyFunction) {
        this.getValueFromKeyFunction = getValueFromKeyFunction;
    }
    
    /**
     * For each key in the passed in map, this function returns true if
     * 
     * <li> the value derived from the key using the function (passed in from the constructor) matches <b>any</b> of the value included for the same key in the map
     * <li> the above holds true for <b>all</b> keys in the map 
     * 
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")    
    public boolean apply(@Nullable Map<String, Collection<String>> input) {
        if (null == input) {
            throw new NullPointerException();
        }
        for (Map.Entry<String, Collection<String>> entry: input.entrySet()) {
            String key = entry.getKey();                
            Collection<String> value = entry.getValue();
            if (!value.contains(getValueFromKeyFunction.apply(key))) {
                return false;
            }
        }
        return true;

    }
}
