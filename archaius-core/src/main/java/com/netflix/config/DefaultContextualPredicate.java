package com.netflix.config;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

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
