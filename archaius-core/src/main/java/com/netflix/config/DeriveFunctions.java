package com.netflix.config;


import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Optional;

public class DeriveFunctions {
    
    private static final String[] TRUE_VALUES =  { "true",  "t", "yes", "y", "on"  };
    private static final String[] FALSE_VALUES = { "false", "f", "no",  "n", "off" };

    public static final Function<String, Integer> DERIVE_INT = new Function<String, Integer>() {        
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Integer apply(@Nullable String input) {
            return Integer.valueOf(input);
        }
        
    };        
    
    public static final Function<String, Boolean> DERIVE_BOOLEAN = new Function<String, Boolean>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Boolean apply(@Nullable String input) {
            for (int i = 0; i < TRUE_VALUES.length; i++){
                if (input.equalsIgnoreCase(TRUE_VALUES[i])) {
                    return Boolean.TRUE;
                }
            }
            for (int i = 0; i < FALSE_VALUES.length; i++){
                if (input.equalsIgnoreCase(FALSE_VALUES[i])) {
                    return Boolean.FALSE;
                }
            }
            throw new IllegalArgumentException();
        }        
    };
    
    public static final Function<String, Long> DERIVE_LONG = new Function<String, Long>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Long apply(@Nullable String input) {
            return Long.valueOf(input);
        }        
    };
    
    public static final Function<String, Float> DERIVE_FLOAT = new Function<String, Float>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Float apply(@Nullable String input) {
            return Float.valueOf(input);
        }        
    };

    public static final Function<String, Double> DERIVE_DOUBLE = new Function<String, Double>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Double apply(@Nullable String input) {
            return Double.valueOf(input);
        }        
    };

    public static final Function<String, String> DERIVE_STRING = new Function<String, String>() {
        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public String apply(@Nullable String input) {
            return input;
        }        
    };

    
    @SuppressWarnings("unchecked")
    public static <T> Optional<Function<String, T>> lookup(Class<T> classType) {
        Function<String, T> functionToReturn = null;
        if (Integer.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_INT;
        } else if (Boolean.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_BOOLEAN;
        } else if (Float.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_FLOAT;
        } else if (Double.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_DOUBLE;       
        } else if (String.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_STRING;
        } else if (Long.class.equals(classType)) {
            functionToReturn = (Function<String, T>) DERIVE_LONG;
        }
        if (functionToReturn != null) {
            return Optional.of(functionToReturn);
        } else {
            return Optional.absent();
        }        
    }
}
