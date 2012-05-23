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

public class ConfigurationUtils {
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
