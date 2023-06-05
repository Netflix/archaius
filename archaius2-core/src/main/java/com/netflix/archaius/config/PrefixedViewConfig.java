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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.StrInterpolator.Lookup;
import com.netflix.archaius.interpolate.ConfigStrLookup;

/**
 * View into another Config for properties starting with a specified prefix.
 *
 * This class is meant to work with dynamic Config object that may have properties
 * added and removed.
 */
public class PrefixedViewConfig extends AbstractDependentConfig {
    private final Config config;
    private final String prefix;
    private final Lookup nonPrefixedLookup;
    private volatile CachedState state;

    /** Listener to update the state of the PrefixedViewConfig on any changes in the source config. */
    private static class PrefixedViewConfigListener extends DependentConfigListener<PrefixedViewConfig> {
        private PrefixedViewConfigListener(PrefixedViewConfig pvc) {
            super(pvc);
        }

        @Override
        public void onSourceConfigAdded(PrefixedViewConfig pvc, Config config) {
            pvc.updateState(config);
        }

        @Override
        public void onSourceConfigRemoved(PrefixedViewConfig pvc, Config config) {
            pvc.updateState(config);
        }

        @Override
        public void onSourceConfigUpdated(PrefixedViewConfig pvc, Config config) {
            pvc.updateState(config);
        }

        @Override
        public void onSourceError(Throwable error, PrefixedViewConfig pvc, Config config) {
        }
    }
    
    public PrefixedViewConfig(final String prefix, final Config config) {
        this.config = config;
        this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        this.nonPrefixedLookup = ConfigStrLookup.from(config);
        this.state = createState(config);
        this.config.addListener(new PrefixedViewConfigListener(this));
    }

    private void updateState(Config config) {
        this.state = createState(config);
    }

    private CachedState createState(Config config) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Config> instrumentedKeys = new LinkedHashMap<>();
        boolean instrumented = config.instrumentationEnabled();
        config.forEachPropertyUninstrumented((k, v) -> {
            if (k.startsWith(prefix)) {
                String key = k.substring(prefix.length());
                data.put(key, v);
                if (instrumented) {
                    instrumentedKeys.put(key, config);
                }
            }
        });
        return new CachedState(data, instrumentedKeys);
    }

    @Override
    public CachedState getState() {
        return state;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        T t = null;
        for (Entry<String, Object> entry : state.getData().entrySet()) {
            t = visitor.visitKey(entry.getKey(), entry.getValue());
        }
        return t;
    }
    
    @Override
    protected Lookup getLookup() { 
        return nonPrefixedLookup; 
    }

    @Override
    public synchronized void setDecoder(Decoder decoder) {
        super.setDecoder(decoder);
        config.setDecoder(decoder);
    }

    @Override
    public synchronized void setStrInterpolator(StrInterpolator interpolator) {
        super.setStrInterpolator(interpolator);
        config.setStrInterpolator(interpolator);
    }

    @Override
    public synchronized void addListener(ConfigListener listener) {
        super.addListener(listener);
        config.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ConfigListener listener) {
        super.removeListener(listener);
        config.removeListener(listener);
    }

    @Override
    protected PropertyDetails createPropertyDetails(String key, Object value) {
        return new PropertyDetails(prefix + key, null, value);
    }
}
