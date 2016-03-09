package com.netflix.archaius.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.MapConfig;

/***
 * JUnit rule which builds an Archaius2 {@link Config} instance using annotations.
 * 
 * <pre>
 *  {@literal @}Rule
 *  public Archaius2TestConfig config = new Archaius2TestConfig();
 *
 *  {@literal @}Test 
 *  {@literal @}TestPropertyOverride({"propName=propValue"})
 *  public void testBasicPropertyResolution() {
 *      assertEquals("propValue", config.get().getString("propName"));
 *  }
 * </pre>
 * 
 * See {@link TestPropertyOverride} for additional usage information.
 */
public class Archaius2TestConfig implements TestRule {
    private final ThreadLocal<Config> configHolder = new ThreadLocal<>();
        
    public Config get() {
        return configHolder.get();
    }
    
    public Statement apply(Statement base, Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    createConfig(description);
                    base.evaluate();
                } finally {
                    cleanupConfig(description);
                }
            }
        };
    }
    
    private void createConfig(Description description) {
        Properties props = new Properties();
        addPropertiesUsingAnnotation(description.getTestClass().getAnnotation(TestPropertyOverride.class), props);
        addPropertiesUsingAnnotation(description.getAnnotation(TestPropertyOverride.class), props);
        configHolder.set(new MapConfig(props));
    }
    
    private void addPropertiesUsingAnnotation(TestPropertyOverride annotation, Properties properties) {
        if(annotation == null || annotation.value().length < 1)
        {
            return;
        }
        for(String override : annotation.value()) {
            String[] parts = override.split("=", 2);
            if(parts.length < 2) {
                throw new TestConfigException("Error parsing TestPropertyOverride for: " 
                        + annotation.value() + " Please ensure you are specifying overrides in the form \"key=value\"");
            }
            properties.put(parts[0], parts[1]);
        }
    }

    private void cleanupConfig(Description description) {
        configHolder.remove();
    }
    
    
    private String getKey(Description description) {
        return description.getClassName()+description.getMethodName()+description.getDisplayName();
    }

}
