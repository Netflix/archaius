/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.archaius.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as a configuration item. Governator will auto-assign the value based
 * on the {@link #value()} of the annotation via the set {@link ConfigurationProvider}.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Configuration
{
    /**
     * @return name/key of the config to assign
     */
    String      prefix() default "";

    /**
     * @return field names to use for replacement
     */
    String[]    params() default {};
    
    /**
     * @return user displayable description of this configuration
     */
    String      documentation() default "";
    
    /**
     * @return true to allow mapping configuration to fields
     */
    boolean     allowFields() default false;
    
    /**
     * @return true to allow mapping configuration to setters
     */
    boolean     allowSetters() default true;
    
    /**
     * @return Method to call after configuration is bound
     */
    String      postConfigure() default "";
    
    /**
     * @return If true then properties cannot change once set otherwise methods will be 
     * bound to dynamic properties via PropertyFactory.
     */
    boolean     immutable() default false;
}
