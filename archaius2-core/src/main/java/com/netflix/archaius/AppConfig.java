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
package com.netflix.archaius;

import java.util.Collection;

import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.exceptions.ConfigException;

/**
 * Core configuration API to be used as the entry point into configuration.
 * 
 * @author elandau
 */
public interface AppConfig extends PropertyFactory, SettableConfig, ConfigLoader {
    /**
     * Return a child layer
     * 
     * @param name
     * @return
     */
    Config getLayer(String name);
    
    /**
     * Return a child layer into which other configurations may be loaded.
     * 
     * TODO: Throw an exception if not composite
     * 
     * @param name
     * @return
     * @throws ConfigException 
     */
    CompositeConfig getCompositeLayer(String name) throws ConfigException;
    
    /**
     * @return List of layer names in order of override priority
     */
    Collection<String> getLayerNames();
}
