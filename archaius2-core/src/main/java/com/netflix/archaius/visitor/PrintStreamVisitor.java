/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.visitor;

import java.io.PrintStream;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.CompositeConfig;

public class PrintStreamVisitor implements CompositeConfig.CompositeVisitor<Void> {
    private final PrintStream stream;
    private String prefix = "";
    
    public static PrintStreamVisitor OUT = new PrintStreamVisitor(System.out);
    
    public PrintStreamVisitor(PrintStream stream) {
        this.stream = stream;
    }
    
    public PrintStreamVisitor() {
        this(System.out);
    }
    
    @Override
    public Void visitKey(String key, Object value) {
        stream.println(prefix + key + " = " + value);
        return null;
    }

    @Override
    public Void visitChild(String name, Config child) {
        stream.println(prefix + "Config: " + name);
        prefix += "  ";
        child.accept(this);
        prefix = prefix.substring(0, prefix.length()-2);
        return null;
    }
}
