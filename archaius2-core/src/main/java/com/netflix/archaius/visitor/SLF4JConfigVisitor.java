package com.netflix.archaius.visitor;

import com.netflix.archaius.log.ArchaiusLogger;
import com.netflix.archaius.log.ArchaiusLoggerFactory;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;

public class SLF4JConfigVisitor implements CompositeConfig.CompositeVisitor<Void> {
    private static final ArchaiusLogger LOG = ArchaiusLoggerFactory.getLogger(SLF4JConfigVisitor.class);
    private final String INDENT_STR = "  ";
    
    private String currentIndent = "";
    
    @Override
    public Void visitKey(Config config, String key) {
        LOG.debug(currentIndent + key + " = " + config.getRawProperty(key));
        return null;
    }

    @Override
    public Void visitChild(String name, Config child) {
        LOG.debug(currentIndent + "Config: " + name);
        currentIndent += INDENT_STR;
        child.accept(this);
        currentIndent = currentIndent.substring(0, currentIndent.length() - INDENT_STR.length());
        return null;
    }
}
