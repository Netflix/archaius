package com.netflix.archaius.visitor;

import java.util.LinkedHashMap;
import java.util.Stack;

import com.google.common.base.Joiner;
import com.netflix.archaius.Config;
import com.netflix.archaius.config.CompositeConfig;

public class PropertyOverrideVisitor implements CompositeConfig.CompositeVisitor<LinkedHashMap<String, String>> {
    public static PrintStreamVisitor OUT = new PrintStreamVisitor(System.out);
    
    private final Stack<String> stack = new Stack<>();
    private final String key;
    private final LinkedHashMap<String, String> hierarchy = new LinkedHashMap<>();
    
    public PropertyOverrideVisitor(String key) {
        this.key = key;
    }
    
    @Override
    public LinkedHashMap<String, String> visit(Config config, String key) {
        return hierarchy;
    }

    @Override
    public LinkedHashMap<String, String> visit(String name, Config child) {
        stack.push(name);
        if (child instanceof CompositeConfig) {
            child.accept(this);
        }
        else {
            Object value = child.getRawProperty(key);
            if (value != null) {
                hierarchy.put(Joiner.on(":").join(stack), value.toString());
            }
        }
        stack.pop();
        return hierarchy;
    }
}