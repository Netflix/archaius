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
package com.netflix.archaius.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special DynamicConfig that reads an entire snapshot of the configuration
 * from a source and performs a delta comparison.  Each new snapshot becomes
 * the new immutable Map backing this config.  
 * 
 * @author elandau
 *
 */
public class PollingDynamicConfig extends AbstractDynamicConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PollingDynamicConfig.class);
    
    private volatile Map<String, String> current = new HashMap<String, String>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Callable<Map<String, String>> reader;
    
    public PollingDynamicConfig(String name, Callable<Map<String, String>> reader, PollingStrategy strategy) {
        super(name);
        
        this.reader = reader;
        strategy.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    update();
                    return true;
                }
                catch (Exception e) {
                    return false;
                }
            }
        });
    }

    @Override
    public boolean containsProperty(String key) {
        return current.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return current.isEmpty();
    }

    @Override
    public String getRawString(String key) {
        return current.get(key);
    }

    private void update() throws IOException {
        // OK to ignore calls to update() if already busy updating 
        if (busy.compareAndSet(false, true)) {
            try {
                Map<String, String> newConfig = reader.call();
                current = newConfig;
                notifyOnUpdate();
            }
            catch (Exception e) {
                try {
                    notifyOnError(e);
                }
                catch (Exception e2) {
                    LOG.warn("Failed to notify error observer for config " + getName());
                }
            }
            finally {
                busy.set(false);
            }
        }
    }

    @Override
    public Iterator<String> getKeys() {
        return current.keySet().iterator();
    }
}
