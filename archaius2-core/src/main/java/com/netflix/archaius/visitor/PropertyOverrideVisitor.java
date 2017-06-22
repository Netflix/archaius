package com.netflix.archaius.visitor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Stack;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;

/**
 * Produce an ordered LinkedHashMap with all instances of a property in the configuration 
 * hierarchy with the key being the 'path' to the property.
 * @author elandau
 *
 */
public class PropertyOverrideVisitor implements CompositeConfig.CompositeVisitor<LinkedHashMap<String, String>> {
    public static PrintStreamVisitor OUT = new PrintStreamVisitor(System.out);
    
    private static final String SEPARATOR = "/";
    
    private final Stack<String> stack = new Stack<>();
    private final String key;
    private final LinkedHashMap<String, String> hierarchy = new LinkedHashMap<>();
    
    public PropertyOverrideVisitor(String key) {
        this.key = key;
    }
    
    @Override
    public LinkedHashMap<String, String> visitKey(String key, Object value) {
        return hierarchy;
    }

    @Override
    public LinkedHashMap<String, String> visitChild(String name, Config child) {
        stack.push(name);
        if (child instanceof CompositeConfig) {
            child.accept(this);
        }
        else {
            Object value = child.getRawProperty(key);
            if (value != null) {
                hierarchy.put(join(stack, SEPARATOR), value.toString());
            }
        }
        stack.pop();
        return hierarchy;
    }
    
    private static String join(Collection<String> values, String sep) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = values.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }
}