package com.netflix.archaius.interpolate;

import com.netflix.archaius.Config;
import com.netflix.archaius.StrInterpolator.Lookup;

/**
 * Interpolator lookup using a Config as the source
 * @author elandau
 *
 */
public class ConfigStrLookup implements Lookup {

    private Config config;

    public ConfigStrLookup(Config config) {
        this.config = config;
    }
    
    @Override
    public String lookup(String key) {
        Object value = config.getRawProperty(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static Lookup from(Config config) {
        return new ConfigStrLookup(config);
    }
    
}
