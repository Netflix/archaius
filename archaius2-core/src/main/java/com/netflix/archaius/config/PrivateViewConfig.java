/**
 * Copyright 2022 Netflix, Inc.
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.StrInterpolator;

/**
 * View into another Config that allows usage of a private {@link Decoder}, {@link StrInterpolator}, and
 * {@link ConfigListener}s that will NOT be shared with the original config.
 * <p>
 * This class is meant to work with dynamic Config object that may have properties added and removed.
 */
public class PrivateViewConfig extends AbstractConfig {

    private static class State {
        final Map<String, Object> data;

        public State(Config config) {
            data = new LinkedHashMap<>();
            config.forEachProperty(data::put);
        }
    }

    /** Listener to update our own state on upstream changes and then propagate the even to our own listeners. */
    private static class ViewConfigListener extends DependentConfigListener<PrivateViewConfig> {
        private ViewConfigListener(PrivateViewConfig dependentConfig) {
            super(dependentConfig);
        }

        @Override
        public void onSourceConfigAdded(PrivateViewConfig pvc, Config config) {
            pvc.state = new State(config);
            pvc.notifyConfigAdded(pvc);
        }

        @Override
        public void onSourceConfigRemoved(PrivateViewConfig pvc, Config config) {
            pvc.state = new State(config);
            pvc.notifyConfigRemoved(pvc);
        }

        @Override
        public void onSourceConfigUpdated(PrivateViewConfig pvc, Config config) {
            pvc.state = new State(config);
            pvc.notifyConfigUpdated(pvc);
        }

        @Override
        public void onSourceError(Throwable error, PrivateViewConfig pvc, Config config) {
        }
    }

    private volatile State state;

    public PrivateViewConfig(final Config wrappedConfig) {
        this.state = new State(wrappedConfig);
        wrappedConfig.addListener(new ViewConfigListener(this));
    }

    @Override
    public Iterator<String> getKeys() {
        return state.data.keySet().iterator();
    }

    @Override
    public boolean containsKey(String key) {
        return state.data.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return state.data.isEmpty();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        T t = null;
        for (Entry<String, Object> entry : state.data.entrySet()) {
            t = visitor.visitKey(entry.getKey(), entry.getValue());
        }
        return t;
    }

    @Override
    public Object getRawProperty(String key) {
        return state.data.get(key);
    }
}
