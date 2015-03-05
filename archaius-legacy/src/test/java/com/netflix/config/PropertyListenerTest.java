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

import static org.junit.Assert.*;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Test;

public class PropertyListenerTest {
    
    @Test
    public void testAddPropertyListener() {
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        config.addConfigurationListener(new ExpandedConfigurationListenerAdapter(new MyListener()));
        // config.addConfigurationListener(new MyListener());
        config.setProperty("xyz", "abcc");
        assertEquals(1, MyListener.count);
    }

}

class MyListener extends AbstractDynamicPropertyListener {
    static int count = 0;

    @Override
    public void handlePropertyEvent(String arg0, Object arg1, EventType arg2) {
        incrementCount();
    }   
    
    private void incrementCount() {
        count++;
    }
}
