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
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * A property that has multiple possible values associated with it and determines
 * the value according to the runtime context, which can include deployment context, 
 * values of other properties or attributes of user input. 
 * <p>
 * The value is defined as a JSON blob that
 * consists of multiple conditions and value associated with each condition. The following
 * is an example:
 * 
 * <pre>{@code
 * [
    {
       "if":  
              {"@environment":["prod"],
               "@region":["us-east-1"]
              },
       "value":5
    },
     {
        "if":
             {"@environment":["test", "dev"]},
        "value":10
     },
    {
        "value":2
    }
]
 * }</pre>
 * 
 * This blob means that

<li>If "@enviornment" is "prod" <b>and</b> "@region" is "us-east-1",  value of the property is integer 5; (<b>Note:</b> if you use ConfigurationManager, "@enviornment" and "@region" are automatically exported as properties from DeploymentContext)
<li>Else if "@environment" is either "test" or "dev", the value is 10;
<li>Otherwise, the default value of the property is 2
 * <p>
 * In order to make this work, a Predicate is needed to determine if the current runtime context matches any of the conditions
 * described in the JSON blob. The predicate can be passed in as a parameter of the constructor, otherwise the default one
 * will interpret each key in the "dimensions" as a key of a DynamicProperty, and the list of values are the acceptable
 * values of the DynamicProperty.
 * <p>
 * For example:<p>
 * 
 * <pre>{@code
 * 
 *      String json = ... // string as the above JSON blob
 *      ConfigurationManager.getConfigInstance().setProperty("@environment", "test"); // no need to do this in real application as property is automatically set 
        ConfigurationManager.getConfigInstance().setProperty("contextualProp", json);
        DynamicContextualProperty<Integer> prop = new DynamicContextualProperty<Integer>("contextualProp", 0);
        prop.get(); // returns 10 as "@environment" == "test" matches the second condition 
 * }</pre>
 * 
 * @author awang
 *
 * @param <T> Data type of the property, e.g., Integer, Boolean
 */
public class DynamicContextualProperty<T> extends PropertyWrapper<T> {
        
    public static class Value<T> {
        private Map<String, Collection<String>> dimensions;
        private T value;
        private String comment;
        private boolean runtimeEval = false;
        
        public Value() {            
        }

        public Value(T value) {
            this.value = value;
        }

        @JsonProperty("if")
        public final Map<String, Collection<String>> getDimensions() {
            return dimensions;
        }
        
        @JsonProperty("if")
        public final void setDimensions(Map<String, Collection<String>> dimensions) {
            this.dimensions = dimensions;
        }

        public final T getValue() {
            return value;
        }
        
        public final void setValue(T value) {
            this.value = value;
        }

        public final String getComment() {
            return comment;
        }

        public final void setComment(String comment) {
            this.comment = comment;
        }

        public final boolean isRuntimeEval() {
            return runtimeEval;
        }

        public final void setRuntimeEval(boolean runtimeEval) {
            this.runtimeEval = runtimeEval;
        }     
    }

    private static Predicate<Map<String, Collection<String>>> defaultPredicate = DefaultContextualPredicate.PROPERTY_BASED;
    
    private final Predicate<Map<String, Collection<String>>> predicate;
    @VisibleForTesting
    volatile List<Value<T>> values;
    private final ObjectMapper mapper = new ObjectMapper();
    
    private final Class<T> classType;
    
    @SuppressWarnings("unchecked")
    public DynamicContextualProperty(String propName, T defaultValue, Predicate<Map<String, Collection<String>>> predicate) {
        super(propName, defaultValue);
        classType = (Class<T>) defaultValue.getClass();
        this.predicate = predicate;
        propertyChangedInternal();
    }
    
    @SuppressWarnings("unchecked")
    public DynamicContextualProperty(String propName, T defaultValue) {
        super(propName, defaultValue);
        classType = (Class<T>) defaultValue.getClass();
        this.predicate = defaultPredicate;
        propertyChangedInternal();
    }

    private final void propertyChangedInternal() {
        if (prop.getString() != null) {
            try {
                values = mapper.readValue(prop.getString(), new TypeReference<List<Value<T>>>(){});
            } catch (Throwable e) {
                // this could be that the property is not set up as JSON based multi-dimensional contextual property
                // and only has one textual value, try to parse this textual value                
                Optional<T> cachedValue = prop.getCachedValue(classType);
                if (cachedValue.isPresent()) {
                    T value = cachedValue.get();
                    values = Lists.newArrayList();
                    values.add(new Value<T>(value));                        
                } else {
                    values = null;
                }
                if (values == null) {
                    throw new RuntimeException("Unable to parse the property value: " + prop.getString(), e);
                }
            }            
        } else {
            values = null;
        }
    }
    
    @Override
    protected final void propertyChanged() {
        propertyChangedInternal();
        propertyChanged(this.getValue());
    }
    
    @Override
    public T getValue() {        
        if (values != null) {
            for (Value<T> v: values) {
                if (v.getDimensions() == null || v.getDimensions().isEmpty()
                        || predicate.apply(v.getDimensions())) {
                    return v.getValue();
                }
            }
        }
        return defaultValue;
    }
}
