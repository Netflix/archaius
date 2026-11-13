package com.netflix.archaius.config;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Layer;
import com.netflix.archaius.api.config.LayeredConfig;
import com.netflix.archaius.util.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;

/**
 * Composite Config with child sources ordered by {@link Layer}s where there can be 
 * multiple configs in each layer, ordered by insertion order.  Layers form an override 
 * hierarchy for property overrides.  Common hierarchies are, 
 * 
 *  Runtime -> Environment -> System -> Application -> Library -> Defaults
 */
public class DefaultLayeredConfig extends AbstractDependentConfig implements LayeredConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultLayeredConfig.class);
    
    private final ConfigListener listener;
    // Refreshes run under child locks, so use CAS rather than taking the parent monitor.
    // Update the field in place so reads need only one volatile load.
    private volatile ImmutableCompositeState state = new ImmutableCompositeState(Collections.emptyList());
    private static final AtomicReferenceFieldUpdater<DefaultLayeredConfig, ImmutableCompositeState> STATE =
            AtomicReferenceFieldUpdater.newUpdater(DefaultLayeredConfig.class, ImmutableCompositeState.class, "state");

    /**
     * Listener to be added to any component configs which updates the config map and triggers updates on all listeners
     * when any of the components are updated.
     */
    private static class LayeredConfigListener extends DependentConfigListener<DefaultLayeredConfig> {
        private LayeredConfigListener(DefaultLayeredConfig config) {
            super(config);
        }

        @Override
        public void onSourceConfigAdded(DefaultLayeredConfig dlc, Config config) {
            dlc.refreshState();
            dlc.notifyConfigUpdated(dlc);
        }

        @Override
        public void onSourceConfigRemoved(DefaultLayeredConfig dlc, Config config) {
            dlc.refreshState();
            dlc.notifyConfigUpdated(dlc);
        }

        @Override
        public void onSourceConfigUpdated(DefaultLayeredConfig dlc, Config config) {
            dlc.refreshState();
            dlc.notifyConfigUpdated(dlc);
        }

        @Override
        public void onSourceError(Throwable error, DefaultLayeredConfig dlc, Config config) {
            dlc.notifyError(error, dlc);
        }
    }
    
    public DefaultLayeredConfig() {
        this(generateUniqueName("layered-"));
    }
    
    public DefaultLayeredConfig(String name) {
        super(name);
        this.listener = new LayeredConfigListener(this);
    }
    
    private void refreshState() {
        STATE.updateAndGet(this, ImmutableCompositeState::refresh);
    }

    @Override
    public synchronized void addConfig(Layer layer, Config config) {
        addConfig(layer, config, insertionOrderCounter.incrementAndGet());
    }
    
    @Override
    public synchronized void addConfig(Layer layer, Config child, int position) {
        LOG.info("Adding property source '{}' at layer '{}'", child.getName(), layer);
        
        if (child == null) {
            return;
        }
        
        addChild(child, listener, state.containsConfig(child),
                () -> STATE.updateAndGet(this, current -> current.addChild(new LayerAndConfig(layer, child, position))));
        notifyConfigUpdated(this);
    }
    
    @Override
    public Collection<Config> getConfigsAtLayer(Layer layer) {
        return state.children.stream()
                .filter(holder -> holder.layer.equals(layer))
                .map(holder -> holder.config)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Optional<Config> removeConfig(Layer layer, String name) {
        LOG.info("Removing property source '{}' from layer '{}'", name, layer);
        ImmutableCompositeState before = state;
        Optional<Config> previous = before.findChild(layer, name);
        if (previous.isPresent()) {
            ImmutableCompositeState updated = STATE.updateAndGet(this, current -> current.removeChild(layer, name));
            Set<Config> detached = Collections.newSetFromMap(new IdentityHashMap<>());
            for (LayerAndConfig source : before.children) {
                detached.add(source.config);
            }
            for (LayerAndConfig source : updated.children) {
                detached.remove(source.config);
            }
            detached.forEach(child -> child.removeListener(listener));
            this.notifyConfigUpdated(this);
        }
        return previous;
    }
    
    private static final AtomicInteger insertionOrderCounter = new AtomicInteger(1);
    
    /**
     * Instance of a single child Config within the composite structure
     */
    private static class LayerAndConfig {
        private final Layer layer;
        private final int internalOrder;
        private final Config config;
        
        private LayerAndConfig(Layer layer, Config config, int internalOrder) {
            this.layer = layer;
            this.internalOrder = internalOrder;
            this.config = config;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 31 + ((config == null) ? 0 : config.hashCode());
            result = prime * result + ((layer == null) ? 0 : layer.hashCode());
            return result;
        }

        public Layer getLayer() {
            return layer;
        }
        
        public Config getConfig() {
            return config;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LayerAndConfig other = (LayerAndConfig) obj;
            if (config == null) {
                if (other.config != null)
                    return false;
            } else if (!config.equals(other.config))
                return false;
            if (layer == null) {
                if (other.layer != null)
                    return false;
            } else if (!layer.equals(other.layer))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Element [layer=" + layer + ", id=" + internalOrder + ", value=" + config + "]";
        }
    }
    
    private static final Comparator<LayerAndConfig> ByPriorityAndInsertionOrder = (LayerAndConfig o1, LayerAndConfig o2) -> {
        if (o1.layer != o2.layer) {
            int result = o1.layer.getOrder() - o2.layer.getOrder();
            if (result != 0) {
                return result;
            }
        }
        
        return o2.internalOrder - o1.internalOrder;
    };

    /**
     * Immutable composite state of the DefaultLayeredConfig.  A new instance of this
     * will be created whenever a new Config is added or removed
     */
    private static final class ImmutableCompositeState {
        private final List<LayerAndConfig> children;
        private final CachedState cachedState;
        
        ImmutableCompositeState(List<LayerAndConfig> entries) {
            this(sortedCopy(entries), 0);
        }

        private static List<LayerAndConfig> sortedCopy(List<LayerAndConfig> entries) {
            // A refresh may reuse a published list while another refresh is iterating it.
            List<LayerAndConfig> sorted = new ArrayList<>(entries);
            sorted.sort(ByPriorityAndInsertionOrder);
            return Collections.unmodifiableList(sorted);
        }

        private ImmutableCompositeState(List<LayerAndConfig> children, int size) {
            this.children = children;
            Map<String, Object> data = size > 0 ? Maps.newHashMap(size) : new HashMap<>();
            Map<String, Config> instrumentedKeys = new HashMap<>();
            for (LayerAndConfig child : children) {
                boolean instrumented = child.config.instrumentationEnabled();
                child.config.forEachPropertyUninstrumented(
                        (k, v) -> updateData(data, instrumentedKeys, k, v, child.config, instrumented));
            }
            this.cachedState = new CachedState(data, instrumentedKeys);
        }

        private void updateData(
                Map<String, Object> data,
                Map<String, Config> instrumentedKeys,
                String key,
                Object value,
                Config childConfig,
                boolean instrumented) {
            if (!data.containsKey(key)) {
                if (instrumented) {
                    instrumentedKeys.put(key, childConfig);
                }
                data.put(key, value);
            }
        }
        
        public ImmutableCompositeState addChild(LayerAndConfig layerAndConfig) {
            List<LayerAndConfig> newChildren = new ArrayList<>(this.children);
            newChildren.add(layerAndConfig);
            return new ImmutableCompositeState(newChildren);
        }

        public ImmutableCompositeState removeChild(Layer layer, String name) {
            List<LayerAndConfig> newChildren = new ArrayList<>(this.children.size());
            this.children.stream()
                .filter(source -> !(source.getLayer().equals(layer) && name.equals(source.getConfig().getName())))
                .forEach(newChildren::add);
            return new ImmutableCompositeState(newChildren);
        }
        
        public Optional<Config> findChild(Layer layer, String name) {
            return children
                    .stream()
                    .filter(source -> source.layer.equals(layer) && source.config.getName().equals(name))
                    .findFirst()
                    .map(LayerAndConfig::getConfig);
        }

        boolean containsConfig(Config config) {
            return children.stream().anyMatch(child -> child.config == config);
        }

        ImmutableCompositeState refresh() {
            // The published child list is already immutable and sorted.
            return new ImmutableCompositeState(children, cachedState.getData().size());
        }
    }

    @Override
    public CachedState getState() {
        return state.cachedState;
    }
}
