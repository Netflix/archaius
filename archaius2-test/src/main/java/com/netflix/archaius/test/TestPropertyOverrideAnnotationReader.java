package com.netflix.archaius.test;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class to read {@literal@}{@link TestPropertyOverride} annotations and
 * transform them into {@link Properties} objects. This is intended for use in 
 * testing utilities such as the {@link Archaius2TestConfig} JUnit Rule. 
 */
public class TestPropertyOverrideAnnotationReader {
    
    public Properties getPropertiesForAnnotation(TestPropertyOverride annotation) {
        Properties properties = new Properties();
        if (annotation == null) {
            return properties;
        }

        for (String fileName : annotation.propertyFiles()) {
            try {
                properties.load(this.getClass().getClassLoader().getResourceAsStream(fileName));
            } catch (IOException e) {
                throw new TestConfigException("Failed to load property file from classpath", e);
            }
        }
        for (String override : annotation.value()) {
            String[] parts = override.split("=", 2);
            if (parts.length < 2) {
                throw new TestConfigException("Error parsing TestPropertyOverride for: " + annotation.value()
                        + " Please ensure you are specifying overrides in the form \"key=value\"");
            }
            properties.put(parts[0], parts[1]);
        }
        return properties;
    }
}
