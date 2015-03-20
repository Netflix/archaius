package com.netflix.archaius.config;

import java.util.List;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;

public class ForwardingConfigListener implements ConfigListener {

    private final List<ConfigListener> listeners;
    private final Config config;

    public ForwardingConfigListener(List<ConfigListener> listeners, Config config) {
        this.listeners = listeners;
        this.config = config;
    }
    
    @Override
    public void onConfigAdded(Config config) {
        onConfigUpdated(config);
    }

    @Override
    public void onConfigRemoved(Config config) {
        onConfigUpdated(config);
    }

    @Override
    public void onConfigUpdated(String propName, Config config) {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated(propName, this.config);
        }
    }

    @Override
    public void onConfigUpdated(Config config) {
        for (ConfigListener listener : listeners) {
            listener.onConfigUpdated(this.config);
        }
    }

    @Override
    public void onError(Throwable error, Config config) {
        for (ConfigListener listener : listeners) {
            listener.onError(error, config);
        }
    }

}
