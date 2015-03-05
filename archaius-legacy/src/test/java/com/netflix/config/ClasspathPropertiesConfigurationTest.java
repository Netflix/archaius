/**
 * Copyright 2014 Netflix, Inc.
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

import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class ClasspathPropertiesConfigurationTest {

    /**
     * Default relative resource path is META-INF/conf/config.properties.
     * All config files that mathc the pattern will be imported and loaded into the configuration.
     * In test/resource directory, there's a standalone META-INF/conf/config.properties file:
     * standaloneConfigProperty = 10
     * There's another jar file which has an embedded META-INF/conf/config.properties file:
     * jarConfigProperty = 11
     * Once initialize() succeeds, both properties should be present in config file.
     */
    @Test
    public void testClasspathConfiguration() throws Exception {
        ClasspathPropertiesConfiguration.initialize();
        assertEquals("10", ConfigurationManager.getConfigInstance().getString("standaloneConfigProperty"));
        assertEquals("11", ConfigurationManager.getConfigInstance().getString("jarConfigProperty"));

        //Set a non-existent config path, expect an IO Exception
        ClasspathPropertiesConfiguration.setPropertiesResourceRelativePath("non-existent/conf/bluh");
        try {
            ClasspathPropertiesConfiguration.initialize();
            fail("exception expected");
        } catch (Exception e) {
            assertEquals("Cannot locate non-existent/conf/bluh as a classpath resource.", e.getCause().getMessage());
        }
    }
        
}