package com.netflix.archaius.typesafe.dynamic;

import com.netflix.archaius.config.polling.PollingResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Loads typesafe configs.
 */
public class TypesafeConfigLoader implements Callable<PollingResponse> {
    private final Logger LOG = LoggerFactory.getLogger(TypesafeConfigLoader.class);
    private Supplier<Config> typesafeConfigSupplier;

    public TypesafeConfigLoader(Supplier<Config> typesafeConfigSupplier) {
        this.typesafeConfigSupplier = typesafeConfigSupplier;
    }

    public PollingResponse call() throws Exception {
        final Map<String, Object> map = new HashMap<>();

        Config typesafeConfig = typesafeConfigSupplier.get();

        for (Map.Entry<String, ConfigValue> entry : typesafeConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();

            if (addAsUnit(map, typesafeConfig, key, value)) continue;

            map.put(key, value);
        }

        return new PollingResponse() {
            @Override
            public Map<String, Object> getToAdd() {
                return map;
            }

            @Override
            public Collection<String> getToRemove() {
                return Collections.emptyList();
            }

            @Override
            public boolean hasData() {
                return map.size() > 0;
            }
        };
    }

    /**
     * Tries to parse the value to a Duration/ConfigMemorySize and add it as that to the map.
     */
    private boolean addAsUnit(Map<String, Object> map, Config typesafeConfig, String key, Object value) {
        if (value instanceof String) {
            String v = (String) value;
            if (StringUtils.isNotBlank(v) && !Character.isDigit(v.charAt(v.length() - 1))) {
                // We're dealing with a unit. Try to parse it to Duration/ConfigMemorySize.
                Optional<Object> parsedValue = getAsUnit(typesafeConfig, key);
                if (parsedValue.isPresent()) {
                    map.put(key, parsedValue.get());
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Object> getAsUnit(Config typesafeConfig, String key) {
        try {
            return Optional.of(typesafeConfig.getDuration(key));
        } catch (Exception e) {
            // could not parse value as Duration.
        }

        try {
            return Optional.of(typesafeConfig.getMemorySize(key));
        } catch (Exception e) {
            // could not parse value as MemorySize.
        }

        return Optional.empty();
    }

}
