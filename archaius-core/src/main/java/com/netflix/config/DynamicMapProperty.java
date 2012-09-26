package com.netflix.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicMapProperty<TKEY, TVAL> extends DynamicStringListProperty {
    private static final Logger logger = LoggerFactory.getLogger(DynamicMapProperty.class);

    private volatile Map<TKEY,TVAL> values;
    
    public DynamicMapProperty(String propName, String defaultValue, String mapEntryDelimiterRegex) {
        super(propName, defaultValue, mapEntryDelimiterRegex);
    }

    public DynamicMapProperty(String propName, Map<TKEY, TVAL> defaultValue, String mapEntryDelimiterRegex) {
        this(propName, (String) null, mapEntryDelimiterRegex);
        if (values == null && defaultValue != null) {
            values = Collections.unmodifiableMap(defaultValue);
        }
    }

    public DynamicMapProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicMapProperty(String propName, Map<TKEY, TVAL> defaultValue) {
        this(propName, defaultValue, DynamicStringListProperty.DEFAULT_DELIMITER);
    }

    public Map<TKEY,TVAL> getMap() {
        return values;
    }

    protected void load() {
        super.load();
        final List<String> strings = super.get();
        if (strings == null) {
            return;
        }
        Map<TKEY,TVAL> map = new HashMap<TKEY,TVAL>(strings.size());
        for (String s : strings) {
            String kv[] = getKeyValue(s);
            if (kv.length == 2) {
                map.put(getKey(kv[0]), getValue(kv[1]));
            } else {
                logger.warn("Ignoring illegal key value pair: " + s);
            }
        }
        values = Collections.unmodifiableMap(map);
    }
    
    protected String[] getKeyValue(String keyValue) {
        return keyValue.split("=");    
    }
    
    protected abstract TKEY getKey(String key);
    
    protected abstract TVAL getValue(String value);    
}
