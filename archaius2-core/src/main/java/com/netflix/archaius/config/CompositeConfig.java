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
package com.netflix.archaius.config;

import java.util.Collection;

import com.netflix.archaius.Config;
import com.netflix.archaius.exceptions.ConfigException;

/**
 * Contract for a Config that is a composite of other Configs
 * 
 * @author elandau
 *
 */
public interface CompositeConfig extends Config {
    public static interface CompositeVisitor {
        void visit(Config child);
    }

    Config getConfig(String name);
    
    void addConfig(Config child) throws ConfigException;

    void addConfigs(Collection<Config> config) throws ConfigException;

    void replaceConfig(Config child) throws ConfigException;
    
    boolean removeConfig(Config child);
    
    Collection<String> getConfigNames();
    

}
