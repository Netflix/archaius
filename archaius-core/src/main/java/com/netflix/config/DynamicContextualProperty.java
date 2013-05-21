package com.netflix.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Predicate;

public class DynamicContextualProperty<T> extends PropertyWrapper<T> {
        
    public static class Value<T> {
        private Map<String, Collection<String>> dimensions;
        private T value;
        
        public Value() {            
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
    
    
    public DynamicContextualProperty(String propName, T defaultValue, Predicate<Map<String, Collection<String>>> predicate) {
        super(propName, defaultValue);        
        this.predicate = predicate;
        propertyChangedInternal();
    }
    
    public DynamicContextualProperty(String propName, T defaultValue) {
        super(propName, defaultValue);
        this.predicate = defaultPredicate;
        propertyChangedInternal();
    }

    
    private final void propertyChangedInternal() {
        if (prop.getString() != null) {
            try {
                values = mapper.readValue(prop.getString(), new TypeReference<List<Value<T>>>(){});
            } catch (Throwable e) {
                throw new RuntimeException("Error deserializing string to value list: " + prop.getString(), e);
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
