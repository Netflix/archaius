package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.PropertyDetails;
import com.netflix.archaius.api.StrInterpolator;

import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * Extendable base class for the dependent config paradigm. Dependent configs are assumed to be configs which inherit
 * values from some number (1+) of parent configs. Dependent configs hold onto caches of the property data and operate
 * independently of the parent configs except in cases of propagation of instrumentation data and property value
 * changes.
 *
 * TODO: Move DependentConfigListener logic here as well?
 */
public abstract class AbstractDependentConfig extends AbstractConfig {

    public AbstractDependentConfig(String name) {
        super(name);
    }

    public AbstractDependentConfig() {
        super();
    }

    abstract CachedState getState();

    void addChild(Config child, ConfigListener listener, boolean alreadyPresent, Runnable updateState) {
        StrInterpolator previousInterpolator = child.getStrInterpolator();
        Decoder previousDecoder = child.getDecoder();
        try {
            child.setStrInterpolator(getStrInterpolator());
            child.setDecoder(getDecoder());
            // Register once per child before snapshotting so overlapping updates invalidate the CAS.
            if (!alreadyPresent) {
                child.addListener(listener);
            }
            updateState.run();
        } catch (RuntimeException | Error failure) {
            if (!alreadyPresent) {
                child.removeListener(listener);
            }
            child.setDecoder(previousDecoder);
            child.setStrInterpolator(previousInterpolator);
            throw failure;
        }
    }

    @Override
    public Object getRawProperty(String key) {
        CachedState state = getState();
        Object value = state.getData().get(key);
        Config config = state.getInstrumentedKeys().get(key);
        if (config != null) {
            config.recordUsage(createPropertyDetails(key, value));
        }
        return value;
    }

    @Override
    public Object getRawPropertyUninstrumented(String key) {
        return getState().getData().get(key);
    }

    /** Return a set of all unique keys tracked by any child of this composite. */
    @Override
    public Iterator<String> getKeys() {
        return getState().getData().keySet().iterator();
    }

    @Override
    public Iterable<String> keys() {
        return getState().getData().keySet();
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        CachedState state = getState();
        state.getData().forEach((k, v) -> {
            Config config = state.getInstrumentedKeys().get(k);
            if (config != null) {
                config.recordUsage(createPropertyDetails(k, v));
            }
            consumer.accept(k, v);
        });
    }

    @Override
    public void forEachPropertyUninstrumented(BiConsumer<String, Object> consumer) {
        getState().getData().forEach(consumer);
    }

    @Override
    public boolean containsKey(String key) {
        return getState().getData().containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return getState().getData().isEmpty();
    }

    @Override
    public void recordUsage(PropertyDetails propertyDetails) {
        CachedState state = getState();
        Config config = state.getInstrumentedKeys().get(propertyDetails.getKey());
        if (config != null) {
            config.recordUsage(createPropertyDetails(propertyDetails.getKey(), propertyDetails.getValue()));
        }
    }

    @Override
    public boolean instrumentationEnabled() {
        // In the case of dependent configs, instrumentation needs to be propagated.
        // So, if any of the parent configs are instrumented, we mark this config as instrumented as well.
        return !getState().getInstrumentedKeys().isEmpty();
    }
    
    protected PropertyDetails createPropertyDetails(String key, Object value) {
        return new PropertyDetails(key, null, value);
    }
}
