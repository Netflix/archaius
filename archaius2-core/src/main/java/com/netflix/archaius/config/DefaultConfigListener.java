package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;

/**
 * Default implementation with noops for all ConfigListener events
 * @author elandau
 *
 */
public class DefaultConfigListener implements ConfigListener {

    @Override
    public void onConfigAdded(Config config) {
    }

    @Override
    public void onConfigRemoved(Config config) {
    }

    @Override
    public void onConfigUpdated(Config config) {
    }

    @Override
    public void onError(Throwable error, Config config) {
    }

}
