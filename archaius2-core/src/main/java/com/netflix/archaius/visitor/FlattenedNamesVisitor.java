package com.netflix.archaius.visitor;

import java.util.ArrayList;
import java.util.List;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;

/**
 * Produce a flattened list of the configuration hierarchy in the order in which properties
 * will be resolved.  Note that the list won't show the actual hierarchy and may contain
 * duplicate names if the same name is used in different child nodes.
 * 
 * @author elandau
 *
 */
public class FlattenedNamesVisitor implements CompositeConfig.CompositeVisitor<List<String>> {
    private final List<String> names = new ArrayList<>();
    
    @Override
    // This will never be called
    public List<String> visitKey(Config config, String key) {
        return names;
    }

    @Override
    public List<String> visitChild(String name, Config child) {
        names.add(name);
        if (child instanceof CompositeConfig) {
            child.accept(this);
        }
        return names;
    }
}
