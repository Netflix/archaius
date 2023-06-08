package com.netflix.archaius.config;

import com.netflix.archaius.api.PropertyDetails;

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

    @Override
    public Object getRawProperty(String key) {
        Object value = getState().getData().get(key);
        if (getState().getInstrumentedKeys().containsKey(key)) {
            getState().getInstrumentedKeys().get(key).recordUsage(createPropertyDetails(key, value));
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
        getState().getData().forEach((k, v) -> {
            if (getState().getInstrumentedKeys().containsKey(k)) {
                getState().getInstrumentedKeys().get(k).recordUsage(createPropertyDetails(k, v));
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
        if (getState().getInstrumentedKeys().containsKey(propertyDetails.getKey())) {
            getState().getInstrumentedKeys().get(propertyDetails.getKey()).recordUsage(propertyDetails);
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
