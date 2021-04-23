/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.api.annotations;

import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.StrInterpolator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Identifier for a configuration source as well as a customizable policy for
 * loading cascaded (or different name variations) of the source.
 * 
 * {@code
 * @ConfigurationSource(value="foo")
 * class Foo {
 * 
 * }
 * @author elandau
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationSource {
    /**
     * List of named sources to load.  This could be a simple name, like 'foo' that is resolved by the 
     * property loaders or including a type, 'properties:foo.properties'.
     * 
     * @return
     */
    String[] value();

    /**
     * Policy for creating variations of the configuration source names to be loaded.
     */
    Class<? extends CascadeStrategy> cascading() default NullCascadeStrategy.class;
    
    static class NullCascadeStrategy implements CascadeStrategy {
        @Override
        public List<String> generate(String resource, StrInterpolator interpolator, StrInterpolator.Lookup lookup) {
            return null;
        }
    }
}
