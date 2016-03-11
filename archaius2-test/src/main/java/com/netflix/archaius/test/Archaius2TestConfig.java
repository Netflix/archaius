package com.netflix.archaius.test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.ClassUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.config.MapConfig;

/***
 * JUnit rule which builds an Archaius2 {@link Config} instance using
 * annotations.
 * 
 * <pre>
 *  {@literal @}Rule
 *  public Archaius2TestConfig config = new Archaius2TestConfig();
 *
 *  {@literal @}Test 
 *  {@literal @}TestPropertyOverride({"propName=propValue"})
 *  public void testBasicPropertyResolution() {
 *      assertEquals("propValue", config.getString("propName"));
 *  }
 * </pre>
 * 
 * See {@link TestPropertyOverride} for additional usage information.
 */
public class Archaius2TestConfig implements TestRule, Config {
    private final ThreadLocal<Config> configHolder = new ThreadLocal<>();

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
        for(Class<?> parentClass : ClassUtils.getAllSuperclasses(description.getTestClass()))
        {
            addPropertiesUsingAnnotation(parentClass.getAnnotation(TestPropertyOverride.class), props);
        }
        addPropertiesUsingAnnotation(description.getTestClass().getAnnotation(TestPropertyOverride.class), props);
        addPropertiesUsingAnnotation(description.getAnnotation(TestPropertyOverride.class), props);
        configHolder.set(new MapConfig(props));
    }

    private void addPropertiesUsingAnnotation(TestPropertyOverride annotation, Properties properties) {
        if (annotation == null) {
            return;
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
    }

    private void cleanupConfig(Description description) {
        configHolder.remove();
    }

    private String getKey(Description description) {
        return description.getClassName() + description.getMethodName() + description.getDisplayName();
    }

    @Override
    public void addListener(ConfigListener listener) {
        configHolder.get().addListener(listener);
    }

    @Override
    public void removeListener(ConfigListener listener) {
        configHolder.get().removeListener(listener);
    }

    @Override
    public Object getRawProperty(String key) {
        return configHolder.get().getRawProperty(key);
    }

    @Override
    public Long getLong(String key) {
        return configHolder.get().getLong(key);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return configHolder.get().getLong(key, defaultValue);
    }

    @Override
    public String getString(String key) {
        return configHolder.get().getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return configHolder.get().getString(key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return configHolder.get().getDouble(key);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return configHolder.get().getDouble(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return configHolder.get().getInteger(key);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return configHolder.get().getInteger(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key) {
        return configHolder.get().getBoolean(key);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return configHolder.get().getBoolean(key, defaultValue);
    }

    @Override
    public Short getShort(String key) {
        return configHolder.get().getShort(key);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return configHolder.get().getShort(key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return configHolder.get().getBigInteger(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return configHolder.get().getBigInteger(key, defaultValue);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return configHolder.get().getBigDecimal(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return configHolder.get().getBigDecimal(key, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return configHolder.get().getFloat(key);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return configHolder.get().getFloat(key, defaultValue);
    }

    @Override
    public Byte getByte(String key) {
        return configHolder.get().getByte(key);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return configHolder.get().getByte(key, defaultValue);
    }

    @Override
    public List<?> getList(String key) {
        return configHolder.get().getList(key);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> type) {
        return configHolder.get().getList(key, type);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        return configHolder.get().getList(key, defaultValue);
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return configHolder.get().get(type, key);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        return configHolder.get().get(type, key, defaultValue);
    }

    @Override
    public boolean containsKey(String key) {
        return configHolder.get().containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return configHolder.get().isEmpty();
    }

    @Override
    public Iterator<String> getKeys() {
        return configHolder.get().getKeys();
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        return configHolder.get().getKeys(prefix);
    }

    @Override
    public Config getPrefixedView(String prefix) {
        return configHolder.get().getPrefixedView(prefix);
    }

    @Override
    public void setStrInterpolator(StrInterpolator interpolator) {
        configHolder.get().setStrInterpolator(interpolator);
    }

    @Override
    public StrInterpolator getStrInterpolator() {
        return configHolder.get().getStrInterpolator();
    }

    @Override
    public void setDecoder(Decoder decoder) {
        configHolder.get().setDecoder(decoder);
    }

    @Override
    public Decoder getDecoder() {
        return configHolder.get().getDecoder();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return configHolder.get().accept(visitor);
    }

}
