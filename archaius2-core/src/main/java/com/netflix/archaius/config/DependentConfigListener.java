package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * ConfigListener for the dependent/wrapper config paradigm. Most notably makes the reference to the dependent config
 * a weak reference and removes this listener from the source config so that any abandoned dependent configs can be
 * properly garbage collected.
 *
 * @param <T> The type of the dependent config
 */
public abstract class DependentConfigListener<T extends AbstractConfig> implements ConfigListener {
    private final Reference<T> dependentConfigRef;

    DependentConfigListener(T dependentConfig) {
        dependentConfigRef = new WeakReference<>(dependentConfig);
    }

    @Override
    public void onConfigAdded(Config config) {
        updateState(config).ifPresent(vc -> onSourceConfigAdded(vc, config));
    }

    @Override
    public void onConfigRemoved(Config config) {
        updateState(config).ifPresent(vc -> onSourceConfigRemoved(vc, config));
    }

    @Override
    public void onConfigUpdated(Config config) {
        updateState(config).ifPresent(vc -> onSourceConfigUpdated(vc, config));
    }

    @Override
    public void onError(Throwable error, Config config) {
        updateState(config).ifPresent(vc -> onSourceError(error, vc, config));
    }

    public abstract void onSourceConfigAdded(T dependentConfig, Config sourceConfig);
    public abstract void onSourceConfigRemoved(T dependentConfig, Config sourceConfig);
    public abstract void onSourceConfigUpdated(T dependentConfig, Config sourceConfig);
    public abstract void onSourceError(Throwable error, T dependentConfig, Config sourceConfig);

    /**
     * Checks that the dependent Config object is still alive, and if so it updates its local state from the wrapped
     * source.
     *
     * @return An Optional with the dependent Config object IFF the weak reference to it is still alive, empty otherwise.
     */
    private Optional<T> updateState(Config updatedSourceConfig) {
        T dependentConfig = dependentConfigRef.get();
        if (dependentConfig != null) {
            return Optional.of(dependentConfig);
        } else {
            // The view is gone, cleanup time!
            updatedSourceConfig.removeListener(this);
            return Optional.empty();
        }
    }
}
