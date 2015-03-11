package com.netflix.archaius.visitor;

import java.io.PrintStream;

import com.netflix.archaius.Config;
import com.netflix.archaius.config.CascadingCompositeConfig;

public class PrintStreamVisitor implements Config.Visitor, CascadingCompositeConfig.CompositeVisitor {
    private final PrintStream stream;
    private String prefix = "";
    
    public PrintStreamVisitor(PrintStream stream) {
        this.stream = stream;
    }
    
    public PrintStreamVisitor() {
        this(System.out);
    }
    
    @Override
    public void visit(Config config, String key) {
        stream.println(prefix + key + " = " + config.getString(key));
    }

    @Override
    public void visit(Config child) {
        stream.println(prefix + "Config: " + child.getName());
        prefix += "  ";
        child.accept(this);
        prefix = prefix.substring(0, prefix.length()-2);
    }
}
