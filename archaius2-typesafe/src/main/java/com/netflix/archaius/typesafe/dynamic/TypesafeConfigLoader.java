package com.netflix.archaius.typesafe.dynamic;

import com.netflix.archaius.config.polling.PollingResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
}
