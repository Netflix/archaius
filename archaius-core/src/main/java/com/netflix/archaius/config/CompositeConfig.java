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

public interface CompositeConfig extends Config {
    public static interface Listener {
        void onConfigAdded(Config child);
    }

    public static interface CompositeVisitor {
        void visit(Config child);
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void addConfigLast(Config child) throws ConfigException;

    void addConfigFirst(Config child) throws ConfigException;

    Collection<String> getChildConfigNames();

    void addConfigsLast(Collection<Config> config) throws ConfigException;

    void addConfigsFirst(Collection<Config> config) throws ConfigException;

    boolean replace(Config child);

    void removeConfig(Config child);
    

}
