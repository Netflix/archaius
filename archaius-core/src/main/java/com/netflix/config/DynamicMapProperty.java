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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public abstract class DynamicMapProperty<TKEY, TVAL> extends DynamicStringListProperty {
    private static final Logger logger = LoggerFactory.getLogger(DynamicMapProperty.class);
    
    private Map<TKEY,TVAL> defaultValuesMap;

    private volatile Map<TKEY,TVAL> values;
    
    public DynamicMapProperty(String propName, String defaultValue, String mapEntryDelimiterRegex) {
        super(propName, defaultValue, mapEntryDelimiterRegex);
        defaultValuesMap = parseMapFromStringList(getDefaultValue());
    }
    
    public DynamicMapProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
        defaultValuesMap = parseMapFromStringList(getDefaultValue());
    }

    public DynamicMapProperty(String propName, Map<TKEY, TVAL> defaultValue, String mapEntryDelimiterRegex) {
        this(propName, (String) null, mapEntryDelimiterRegex);
        // Make a defensive copy of the default value. Would prefer to use an ImmutableMap, but that
        // does not allow for null values in the Map.
        defaultValuesMap = (defaultValue == null ? null : Collections.unmodifiableMap(Maps.newLinkedHashMap(defaultValue)));
        
        // The values will be initialized as an empty map in the super constructor
        if (defaultValuesMap != null && values.isEmpty()) {
            values = defaultValuesMap;
        }
    }

    public DynamicMapProperty(String propName, Map<TKEY, TVAL> defaultValue) {
        this(propName, defaultValue, DynamicStringListProperty.DEFAULT_DELIMITER);
    }

    public Map<TKEY,TVAL> getMap() {
        return values;
    }
    
    public Map<TKEY,TVAL> getDefaultValueMap() {
        return defaultValuesMap;
    }

    protected void load() {
        super.load();
        values = parseMapFromStringList(super.get());
    }
    
    protected Map<TKEY,TVAL> parseMapFromStringList(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<TKEY,TVAL> map = new LinkedHashMap<TKEY,TVAL>(strings.size());
        for (String s : strings) {
            String kv[] = getKeyValue(s);
            if (kv.length == 2) {
                map.put(getKey(kv[0]), getValue(kv[1]));
            } else {
                logger.warn("Ignoring illegal key value pair: " + s);
            }
        }
        return Collections.unmodifiableMap(map);
    }
    
    protected String[] getKeyValue(String keyValue) {
        return keyValue.split("=");    
    }
    
    protected abstract TKEY getKey(String key);
    
    protected abstract TVAL getValue(String value);    
}
