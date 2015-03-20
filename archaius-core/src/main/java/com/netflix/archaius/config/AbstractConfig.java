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
import java.util.concurrent.CopyOnWriteArrayList;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.Decoder;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.interpolate.CommonsStrInterpolatorFactory;

public abstract class AbstractConfig implements Config {

    private final String name;
    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();
    private Decoder decoder;
    private StrInterpolator interpolator;
    
    public AbstractConfig(String name) {
        this.name = name;
        this.decoder = new DefaultDecoder();
        this.interpolator = CommonsStrInterpolatorFactory.INSTANCE.create(this);
    }
    
    protected CopyOnWriteArrayList<ConfigListener> getListeners() {
        return listeners;
    }
    
    @Override
    final public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }
    
    @Override
    final public Decoder getDecoder() {
        return this.decoder;
    }
    
    @Override
    final public StrInterpolator getStrInterpolator() {
        return this.interpolator;
    }
    
    @Override
    final public void setStrInterpolator(StrInterpolator interpolator) {
        this.interpolator = interpolator;
    }
    
    @Override
    final public void addListener(ConfigListener listener) {
        listeners.add(listener);
    }
    
    @Override
    final public void removeListener(ConfigListener listener) {
        listeners.remove(listener);
    }
    
    protected void notifyConfigUpdated(String key) {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated(key, this);
        }
    }
    
    protected void notifyConfigUpdated() {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated(this);
        }
    }
    
    protected void notifyError(Throwable t) {
        for (ConfigListener listener : listeners) {
            listener.onError(t, this);
        }
    }
    
    public void notifyConfigAdded(Config child) {
        for (ConfigListener listener : listeners) {
            listener.onConfigAdded(child);
        }
    }

    public void notifyConfigRemoved(Config child) {
        for (ConfigListener listener : listeners) {
            listener.onConfigRemoved(child);
        }
    }

    @Override
    public String interpolate(String key) {
        String value = getRawString(key);
        if (value == null) {
            return null;    // TODO: Should this thrown an exception?
        }
        return interpolator.resolve(value);
    }
    
    @Override
    public Iterator<String> getKeys(String prefix) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        
        return result.iterator();
    }

    @Override
    public Config getPrefixedView(String prefix) {
        return new PrefixedViewConfig(prefix, this);
    }

    @Override
    public void accept(Visitor visitor) {
        Iterator<String> iter = getKeys();
        while (iter.hasNext()) {
            visitor.visit(this, iter.next());
        }
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public String getName() {
        return name;
    }
}
