package com.netflix.archaius.property;

import com.netflix.archaius.api.PropertyListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Globally managed list of listeners.  Listeners are tracked globally as 
 * an optimization so it is not necessary to iterate through all property
 * containers when the listeners need to be invoked since the expectation
 * is to have far less listeners than property containers.
 */
public class ListenerManager {
    public static interface ListenerUpdater {
        public void update();
    }

    private final ConcurrentMap<PropertyListener<?>, ListenerUpdater> lookup = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ListenerUpdater> updaters = new CopyOnWriteArrayList<>();

    public void add(PropertyListener<?> listener, ListenerUpdater updater) {
        lookup.put(listener, updater);
        updaters.add(updater);
    }

    public void remove(PropertyListener<?> listener) {
        ListenerUpdater updater = lookup.remove(listener);
        if (updater != null) {
            updaters.remove(updater);
        }
    }

    public void updateAll() {
        updaters.forEach(ListenerUpdater::update);
    }
}
