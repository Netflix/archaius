/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.typesafe;

import com.netflix.archaius.config.AbstractConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigUtil;
import com.typesafe.config.ConfigValue;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class TypesafeConfig extends AbstractConfig {

    private final Config config;

    public TypesafeConfig(Config config) {
        this.config = config;
    }

    @Override
    public boolean containsKey(String key) {
        return config.hasPath(quoteKey(key));
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        // TODO: Handle lists
        try {
            return config.getValue(quoteKey(key)).unwrapped().toString();
        } catch (ConfigException.Missing ex) {
            return null;
        }
    }

    public List getList(String key) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private String quoteKey(String key) {
        final String[] path = key.split("\\.");
        return ConfigUtil.joinPath(path);
    }

    private String unquoteKey(String key) {
        final List<String> path = ConfigUtil.splitPath(key);
        StringBuilder buf = new StringBuilder();
        buf.append(path.get(0));
        for (String p : path.subList(1, path.size())) {
            buf.append('.').append(p);
        }
        return buf.toString();
    }

    @Override
    public Iterator<String> getKeys() {
        return new Iterator<String>() {
            Iterator<Entry<String, ConfigValue>> iter = config.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public String next() {
                return unquoteKey(iter.next().getKey());
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }
}
