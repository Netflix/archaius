package com.netflix.archaius.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.ClassUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;

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
public class Archaius2TestConfig implements TestRule, SettableConfig {
    private TestCompositeConfig testCompositeConfig;
    private final TestPropertyOverrideAnnotationReader annotationReader = new TestPropertyOverrideAnnotationReader();

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SettableConfig runtimeLevelProperties = new DefaultSettableConfig();
                SettableConfig classLevelProperties = new DefaultSettableConfig();
                SettableConfig methodLevelProperties = new DefaultSettableConfig();
                List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(description.getTestClass());
                Collections.reverse(allSuperclasses);
                for(Class<?> parentClass : allSuperclasses) {
                    classLevelProperties.setProperties(annotationReader.getPropertiesForAnnotation(parentClass.getAnnotation(TestPropertyOverride.class)));
                }               
                classLevelProperties.setProperties(annotationReader.getPropertiesForAnnotation(description.getTestClass().getAnnotation(TestPropertyOverride.class)));
                methodLevelProperties.setProperties(annotationReader.getPropertiesForAnnotation(description.getAnnotation(TestPropertyOverride.class)));                    
                testCompositeConfig = new TestCompositeConfig(runtimeLevelProperties, classLevelProperties, methodLevelProperties);
                base.evaluate();
             }
        };
    }  

    private String getKey(Description description) {
        return description.getClassName() + description.getMethodName() + description.getDisplayName();
    }

    @Override
    public void addListener(ConfigListener listener) {
        testCompositeConfig.addListener(listener);
    }

    @Override
    public void removeListener(ConfigListener listener) {
        testCompositeConfig.removeListener(listener);
    }

    @Override
    public Object getRawProperty(String key) {
        return testCompositeConfig.getRawProperty(key);
    }

    @Override
    public Long getLong(String key) {
        return testCompositeConfig.getLong(key);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return testCompositeConfig.getLong(key, defaultValue);
    }

    @Override
    public String getString(String key) {
        return testCompositeConfig.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return testCompositeConfig.getString(key, defaultValue);
    }

    @Override
    public Double getDouble(String key) {
        return testCompositeConfig.getDouble(key);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return testCompositeConfig.getDouble(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return testCompositeConfig.getInteger(key);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return testCompositeConfig.getInteger(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key) {
        return testCompositeConfig.getBoolean(key);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return testCompositeConfig.getBoolean(key, defaultValue);
    }

    @Override
    public Short getShort(String key) {
        return testCompositeConfig.getShort(key);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return testCompositeConfig.getShort(key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return testCompositeConfig.getBigInteger(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return testCompositeConfig.getBigInteger(key, defaultValue);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return testCompositeConfig.getBigDecimal(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return testCompositeConfig.getBigDecimal(key, defaultValue);
    }

    @Override
    public Float getFloat(String key) {
        return testCompositeConfig.getFloat(key);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return testCompositeConfig.getFloat(key, defaultValue);
    }

    @Override
    public Byte getByte(String key) {
        return testCompositeConfig.getByte(key);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return testCompositeConfig.getByte(key, defaultValue);
    }

    @Override
    public List<?> getList(String key) {
        return testCompositeConfig.getList(key);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> type) {
        return testCompositeConfig.getList(key, type);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        return testCompositeConfig.getList(key, defaultValue);
    }

    @Override
    public <T> T get(Class<T> type, String key) {
        return testCompositeConfig.get(type, key);
    }

    @Override
    public <T> T get(Class<T> type, String key, T defaultValue) {
        return testCompositeConfig.get(type, key, defaultValue);
    }

    @Override
    public boolean containsKey(String key) {
        return testCompositeConfig.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return testCompositeConfig.isEmpty();
    }

    @Override
    public Iterator<String> getKeys() {
        return testCompositeConfig.getKeys();
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        return testCompositeConfig.getKeys(prefix);
    }

    @Override
    public Config getPrefixedView(String prefix) {
        return testCompositeConfig.getPrefixedView(prefix);
    }

    @Override
    public void setStrInterpolator(StrInterpolator interpolator) {
        testCompositeConfig.setStrInterpolator(interpolator);
    }

    @Override
    public StrInterpolator getStrInterpolator() {
        return testCompositeConfig.getStrInterpolator();
    }

    @Override
    public void setDecoder(Decoder decoder) {
        testCompositeConfig.setDecoder(decoder);
    }

    @Override
    public Decoder getDecoder() {
        return testCompositeConfig.getDecoder();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return testCompositeConfig.accept(visitor);
    }

    @Override
    public void setProperties(Config config) {
        testCompositeConfig.setProperties(config);
    }

    @Override
    public void setProperties(Properties properties) {
        testCompositeConfig.setProperties(properties);
    }

    @Override
    public <T> void setProperty(String propName, T propValue) {
        testCompositeConfig.setProperty(propName, propValue);
    }

    @Override
    public void clearProperty(String propName) {
        testCompositeConfig.clearProperty(propName);
    }

    @Override
    public void forEach(BiConsumer<String, Object> consumer) {
        testCompositeConfig.forEach(consumer);
    }
}
