package com.netflix.archaius.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.StrInterpolator;

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
    public List<String> generate(String name, StrInterpolator interpolator) {
        ArrayList<String> result = new ArrayList<String>();
        
        result.add(name);
        
        String current = name;
        for (String param : parameters) {
            current += separator + param;
            result.add(interpolator.resolve(current).toString());
        }
        
        return result;
    }

}
