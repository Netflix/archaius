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

import java.util.Iterator;
import java.util.LinkedHashSet;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;

/**
 * View into another Config for properties starting with a specified prefix.
 * 
 * This class is meant to work with dynamic Config object that may have properties
 * added and removed.
 * 
 * @author elandau
 *
 */
public class PrefixedViewConfig extends AbstractConfig {
    private final Config config;
    private final String prefix;
    
    public PrefixedViewConfig(String prefix, Config config) {
        this.config = config;
        this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
    }

    @Override
    public Iterator<String> getKeys() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.startsWith(prefix)) {
                result.add(key.substring(prefix.length()));
            }
        }
        return result.iterator();
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(prefix + key);
    }

    @Override
    public boolean isEmpty() {
        // This is terribly inefficient
        return !config.getKeys().hasNext();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return config.accept(visitor);
    }

    @Override
    public Object getRawProperty(String key) {
        if (config.containsKey(prefix + key)) {
            return config.getRawProperty(prefix + key);
        }
        return null;
    }
    
    @Override
    public synchronized void setDecoder(Decoder decoder)
    {
        super.setDecoder(decoder);
        config.setDecoder(decoder);
    }

    @Override
    public synchronized void setStrInterpolator(StrInterpolator interpolator)
    {
        super.setStrInterpolator(interpolator);
        config.setStrInterpolator(interpolator);
    }

    @Override
    public synchronized void addListener(ConfigListener listener)
    {
        super.addListener(listener);
        config.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ConfigListener listener)
    {
        super.removeListener(listener);
        config.removeListener(listener);
    }
}
