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
package com.netflix.archaius.interpolate;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.netflix.archaius.Config;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.StrInterpolatorFactory;

public class CommonsStrInterpolatorFactory implements StrInterpolatorFactory {
    
    public static CommonsStrInterpolatorFactory INSTANCE = new CommonsStrInterpolatorFactory();
    
    @Override
    public StrInterpolator create(final Config rootConfig) {
        return new InternalInterpolator(rootConfig);
    }
    
    class InternalInterpolator extends StrLookup implements StrInterpolator {
        private final Config config;
        private final StrSubstitutor sub;
        
        public InternalInterpolator(Config config) {
            this.config = config;
            this.sub = new StrSubstitutor(this, "${", "}", '$');
        }
        
        @Override
        public String resolve(String key) {
            String obj = sub.replace(key);
            return (obj == null) ? key : obj;
        }

        @Override
        public String lookup(String key) {
            return config.getRawString(key);
        }
    }
}
