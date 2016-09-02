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
 *      {@literal @}TestPropertyOverride(value={"propName=propValue", "propName2=propVal2", ... }, propertyFiles={someFile.properties})
 *      
 * </pre>
 * TestPropertyOverride's may be set at the test class, parent test class, and test method levels. 
 * Overrides specified at the test method level take precedance over properties set at class level.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestPropertyOverride {
    
    /**
     * Create properties inline.
     * ex. {@literal @}TestPropertyOverride({"propName=propValue", "propName2=propVal2", ... }) 
     * 
     * These properties will precedance over those created from the
     * propertyFiles attribute in the event of conflicts. 
     */
    String[] value() default {};
    
    /***
     * Use a file location to create properties.
     * ex. {@literal @}TestPropertyOverride(propertyFiles={"unittest.properties"})
     * 
     * These properties will be overwritten by those created from the
     * value attribute in the event of conflicts. 
     */
    String[] propertyFiles() default {};

}
