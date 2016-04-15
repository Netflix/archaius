package com.netflix.archaius.typesafe.dynamic;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

public class DefaultTypesafeClientConfig implements TypesafeClientConfig {
    private final Logger LOG = LoggerFactory.getLogger(DefaultTypesafeClientConfig.class);
    private final String configFilePath;
    private final int refreshIntervalMs;

    public DefaultTypesafeClientConfig(Builder builder) {
        this.configFilePath = builder.configFilePath;
        this.refreshIntervalMs = builder.refreshIntervalMs;
    }

    public static class Builder {
        private String configFilePath;
        private int refreshIntervalMs;

        public Builder withConfigFilePath(String configFilePath) {
            this.configFilePath = configFilePath;
            return this;
        }

        public Builder withRefreshIntervalMs(int refreshIntervalMs) {
            this.refreshIntervalMs = refreshIntervalMs;
            return this;
        }

        public DefaultTypesafeClientConfig build() {
            return new DefaultTypesafeClientConfig(this);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getRefreshRateMs() {
        return refreshIntervalMs;
    }

    @Override
    public String getTypesafeConfigPath() {
        return configFilePath;
    }

    @Override
    public Supplier<Config> getTypesafeConfigSupplier() {
        return new Supplier<Config>() {
            @Override
            public Config get() {
                String typesafeConfigPath = getTypesafeConfigPath();
                LOG.info("Loading typesafe config from: {}", typesafeConfigPath);
                try {
                    return ConfigFactory.parseURL(new URL(typesafeConfigPath),
                            ConfigParseOptions.defaults().setAllowMissing(false)).resolve();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public String toString() {
        return "DefaultTypesafeClientConfig { configFilePath: " + configFilePath +
                ", refreshIntervalMs: " + refreshIntervalMs + " }";
    }
}
