/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.config;

import static org.junit.Assert.*;

import org.junit.Test;

import com.netflix.config.DynamicURLConfiguration;
import com.netflix.config.sources.URLConfigurationSource;

public class DynamicURLConfigurationTest {

    @Test
    public void testNoURLsAvailable() {
        try {
            DynamicURLConfiguration config = new DynamicURLConfiguration();
            assertFalse(config.getKeys().hasNext());
        } catch (Throwable e) {
            fail("Unexpected exception");
        }
        
    }
}
