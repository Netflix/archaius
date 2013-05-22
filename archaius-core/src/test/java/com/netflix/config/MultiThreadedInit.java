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

import java.util.Random;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

public class MultiThreadedInit {


    @Test
    public void test() {
        final BaseConfiguration baseConfig = new BaseConfiguration();
        baseConfig.setProperty("abc", 1);
        (new Thread() {
            public void run() {
                Random r = new Random();
                while (DynamicPropertyFactory.getBackingConfigurationSource() != baseConfig) {
                    try {
                        Thread.sleep(r.nextInt(100) + 1);
                    } catch (InterruptedException e) {} 
                    System.setProperty(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG, "true");
                    ConfigurationManager.install(baseConfig);
                }
            }
        }).start();
        Object config = null;
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty("abc", 0);
        while ((config = DynamicPropertyFactory.getBackingConfigurationSource()) != baseConfig && prop.get() != 1) {            
            // prop = DynamicPropertyFactory.getInstance().getIntProperty("abc", 0);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
        
    }
}
