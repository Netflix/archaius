package com.netflix.archaius.typesafe.dynamic;

import com.typesafe.config.Config;

import java.util.function.Supplier;

/**
 * Provides information on how to retrieve dynamic typesafe configs.
 */
public interface TypesafeClientConfig {
    /**
     * @return True if the client is enabled.  This is checked only once at startup.
     */
    boolean isEnabled();

    /**
     * @return Polling rate for getting updates
     */
    int getRefreshRateMs();

    String getTypesafeConfigPath();

    /**
     *
     * It's up to the user to decide what typesafe Config they want to inject in archaius.
     * This method returns a Supplier. The Supplier<Config>.get() will be called on every polling iteration in order
     * to retrieve the latest typesafe config.
     *
     */
    Supplier<Config> getTypesafeConfigSupplier();
}