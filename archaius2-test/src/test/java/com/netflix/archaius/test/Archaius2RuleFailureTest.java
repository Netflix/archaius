package com.netflix.archaius.test;

import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class Archaius2RuleFailureTest {

    @Test(expected = TestConfigException.class)
    public void testFileNotFoundExceptionThrownWhenNoPropFileFound() throws Throwable {

        Archaius2TestConfig conf = new Archaius2TestConfig();

        // Annotation instance pointing to non-existent prop file
        Annotation testAnnotation = new TestPropertyOverride() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return TestPropertyOverride.class;
            }

            @Override
            public String[] value() {
                return null;
            }

            @Override
            public String[] propertyFiles() {
                return new String[] { "doesNotExist.properties" };
            }
        };

        //No-op statement
        Statement base = new Statement() {
            @Override
            public void evaluate() throws Throwable {
            }
        };

        Statement rule = conf.apply(base, Description.createTestDescription(getClass(), "test", testAnnotation));
        rule.evaluate();
        fail("Should've thrown an exception");
    }
}
