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

public class DeploymentContextInitTest {
    public static class MyDeploymentContext extends SimpleDeploymentContext {
        @Override
        public String getDeploymentEnvironment() {
            return "myEnv";
        }
    }
    
    @Test
    public void testDeploymentContextInit() {
        System.setProperty("archaius.default.deploymentContext.class", MyDeploymentContext.class.getName());
        assertTrue(ConfigurationManager.getDeploymentContext() instanceof MyDeploymentContext);
        assertEquals("myEnv", ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
    }
}
