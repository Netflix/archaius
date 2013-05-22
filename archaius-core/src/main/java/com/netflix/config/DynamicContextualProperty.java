package com.netflix.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class DynamicContextualProperty<T> extends PropertyWrapper<T> {
        
    public static class Value<T> {
        private Map<String, Collection<String>> dimensions;
        private T value;
        
        public Value() {            
        }

        public Value(T value) {
            this.value = value;
        }

        public final Map<String, Collection<String>> getDimensions() {
            return dimensions;
        }
        
        public final void setDimensions(Map<String, Collection<String>> dimensions) {
            this.dimensions = dimensions;
        }
        
        public final T getValue() {
            return value;
        }
        
        public final void setValue(T value) {
            this.value = value;
        }     
    }

    private static Predicate<Map<String, Collection<String>>> defaultPredicate = DefaultContextualPredicate.PROPERTY_BASED;
    
    private final Predicate<Map<String, Collection<String>>> predicate;
    private volatile List<Value<T>> values;
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
                Optional<Function<String, T>> deriveFunction = DeriveFunctions.lookup(classType);
                if (deriveFunction.isPresent()) {
                    try {
                        T value = deriveFunction.get().apply(prop.getString());
                        values = Lists.newArrayList();
                        values.add(new Value<T>(value));    
                    } catch (Exception ex) {
                        values = null;
                        throw new RuntimeException("Unable to recognize the value " + prop.getString() 
                                + " as either JSON or text convertable to type " + classType, ex);
                    }
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
