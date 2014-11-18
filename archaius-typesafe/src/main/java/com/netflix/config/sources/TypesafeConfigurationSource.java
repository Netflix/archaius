package com.netflix.config.sources;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypesafeConfigurationSource implements PolledConfigurationSource
{
    private static final Logger log = LoggerFactory.getLogger(TypesafeConfigurationSource.class);

    @Override
	public PollResult poll(boolean initial, Object checkPoint) throws Exception {
		Map<String, Object> map = load();
		return PollResult.createFull(map);
	}

	synchronized Map<String, Object> load() throws Exception {
        Config config = config();

        Map<String, Object> map = new HashMap<String, Object>();

        for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();

            if (value instanceof List) {
                if (false == safeArrayKeyExpansion(config, key)) {
                    log.error("Unable to expand array: {}", key);
                    continue;
                }

                List values = (List) value;

                map.put(lengthKey(key), values.size());

                for (int i = 0; i < values.size(); i++) {
                    map.put(indexedKey(key, i), values.get(i));
                }
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    private boolean safeArrayKeyExpansion(Config config, String prefix) {
        if (config.hasPath(lengthKey(prefix))) {
            return false;
        }

        // don't need to test element expansion, as "[]" are illegal key characters in Typesafe Config

        return true;
    }

    private String lengthKey(String prefix) {
        return prefix + ".length";
    }

    private String indexedKey(String prefix, int index) {
        return String.format("%s[%d]", prefix, index);
    }

    protected Config config() {
        return ConfigFactory.load();
    }
}