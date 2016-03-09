package com.netflix.archaius.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.netflix.archaius.api.Config;

/***
 * Annotation used in conjunction with {@link Archaius2TestConfig} to create an 
 * Archaius2 {@link Config} instance for testing.
 * 
 * Property values must be specified in the form of:
 * <pre>
 *      {@literal @}TestPropertyOverride({"propName=propValue", "propName2=propVal2", ... })
 * </pre>
 * TestPropertyOverride's may be set at the test class and test method level. 
 * Overrides specified at the test method level take precedance over properties 
 * set at class level.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface TestPropertyOverride {
    
    String[] value() default {};

}
