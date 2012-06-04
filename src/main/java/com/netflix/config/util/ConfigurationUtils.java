/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;

/**
 * Utility class for configuration.
 * 
 * @author awang
 *
 */
public class ConfigurationUtils {
    /**
     * Convert CombinedConfiguration into {@link ConcurrentCompositeConfiguration} as the later has better performance
     * and thread safety. 
     * 
     * @param config Configuration to be converted
     */
    public static ConcurrentCompositeConfiguration convertToConcurrentCompositeConfiguration(CombinedConfiguration config) {
        ConcurrentCompositeConfiguration root = new ConcurrentCompositeConfiguration();
        IdentityHashMap<Configuration, String> reverseMap = new IdentityHashMap<Configuration, String>();
        for (String name: (Set<String>) config.getConfigurationNames()) {
            Configuration child = config.getConfiguration(name);
            reverseMap.put(child, name);
        } 
        for (int i = 0; i < config.getNumberOfConfigurations(); i++) {
            Configuration child = config.getConfiguration(i);
            String name = reverseMap.get(child);
            if (child instanceof CombinedConfiguration) {
                CombinedConfiguration combinedConf = (CombinedConfiguration) child;
                ConcurrentCompositeConfiguration newConf = convertToConcurrentCompositeConfiguration(combinedConf);
                root.addConfiguration(newConf, name);
            } else {
                Configuration conf = new ConcurrentMapConfiguration(child);
                root.addConfiguration((AbstractConfiguration) conf, name);
            }
        }
        return root;
    }
    
    /**
     * Gets all named sub-configuration from a configuration in a map. This method examines each sub-configuration
     * which is an instance of {@link ConcurrentCompositeConfiguration} or CombinedConfiguration and extract the 
     * named configurations out of them.
     *  
     * @param conf Configuration to get all the named sub-configurations
     * @return map where key is the name of the sub-configuration and value is the sub-configuration
     */
    public static Map<String, Configuration> getAllNamedConfiguration(Configuration conf) {
        List<Configuration> toProcess = new ArrayList<Configuration>();
        Map<String, Configuration> map = new HashMap<String, Configuration>();
        toProcess.add(conf);
        while (!toProcess.isEmpty()) {
            Configuration current = toProcess.remove(0);
            if (current instanceof ConcurrentCompositeConfiguration) {
                ConcurrentCompositeConfiguration composite = (ConcurrentCompositeConfiguration) current;
                for (String name: composite.getConfigurationNames()) {
                    map.put(name, composite.getConfiguration(name));
                }
                for (Configuration c: composite.getConfigurations()) {
                    toProcess.add(c);
                }
            } else if (current instanceof CombinedConfiguration) {
                CombinedConfiguration combined = (CombinedConfiguration) current;
                for (String name: (Set<String>) combined.getConfigurationNames()) {
                    map.put(name, combined.getConfiguration(name));
                }
                for (int i = 0; i < combined.getNumberOfConfigurations(); i++) {
                    toProcess.add(combined.getConfiguration(i));
                }
            }
        }
        return map;
    }
    
}
