package com.netflix.config.source;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;
import org.boon.core.Handler;
import org.boon.etcd.Etcd;
import org.boon.etcd.Node;
import org.boon.etcd.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Implementation of the dynamic {@link WatchedConfigurationSource} for Etcd
 *
 * This implementation requires the path to the Etcd directory that contains
 * nodes that represent each managed configuration property.
 * 
 * An example Etcd configuration path is /<my-app>/config
 * An example Etcd property node path is /<my-app>/config/com.fluxcapacitor.my.property
 * 
 * When a property is mutated via Etcd a callback will be notified and the value managed
 * by EtcdConfigurationSource will be updated. Similar to other dynamic configuration
 * source (ie. DynamoDB, etc.)
 * 
 * @author spoon16
 */
public class EtcdConfigurationSource implements WatchedConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(EtcdConfigurationSource.class);
    private static final Splitter keySplitter = Splitter.on('/');

    private final Map<String, Object> valueCache = Maps.newConcurrentMap();
    private final List<WatchedUpdateListener> listeners = new CopyOnWriteArrayList<WatchedUpdateListener>();

    private final Etcd etcd;
    private final String configPath;

    private Handler<Response> updateHandler = new Handler<Response>() {
        @Override
        public void handle(Response updateResponse) {
            if (updateResponse.wasError()) {
                logger.error("Etcd failed with an error response: %s", updateResponse);
            }

            final Map<String, Object> create = Maps.newHashMap();
            final Map<String, Object> set = Maps.newHashMap();
            final Map<String, Object> delete = Maps.newHashMap();

            final String action = updateResponse.action().toLowerCase();
            final Node node = updateResponse.node();

            if (node != null ) {
                final String etcdKey = node.key();
                final String sourceKey = Iterables.getLast(keySplitter.split(etcdKey));
                final String value = node.getValue();
                valueCache.put(sourceKey, value);

                switch (action) {
                    case "create":
                        create.put(sourceKey, value);
                        break;

                    case "set":
                        set.put(sourceKey, value);
                        break;

                    case "delete":
                        delete.put(sourceKey, value);
                        break;

                    default:
                        logger.warn("unrecognized action, response: %s", updateResponse);
                        break;
                }

                final WatchedUpdateResult result = WatchedUpdateResult.createIncremental(create, set, delete);
                updateConfiguration(result);
            }

            etcd.waitRecursive(updateHandler, configPath);
        }
    };

    /**
     * Initialize EtcdConfigurationSource with property values @ configPath
     *
     * @param Etcd etcd
     */
    public EtcdConfigurationSource(Etcd etcd, String configPath) {
        this.etcd = etcd;
        this.configPath = Objects.firstNonNull(configPath, "").replaceAll("^/+","");
        init();
    }

    private void init() {
        final Response listResponse = etcd.list(configPath);
        cacheValues(listResponse.node());
        etcd.waitRecursive(updateHandler, configPath);
    }

    private void cacheValues(Node configNode) {
        for (Node valueNode : configNode.getNodes()) {
            final String etcdKey = valueNode.key();
            final String sourceKey = Iterables.getLast(keySplitter.split(etcdKey));
            final String value = valueNode.getValue();
            valueCache.put(sourceKey, value);
        }
    }
    
    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        return valueCache;
    }

    @Override
    public void addUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    private void updateConfiguration(WatchedUpdateResult result) {
        for (WatchedUpdateListener l : listeners) {
            try {
                l.updateConfiguration(result);
            } catch (Throwable ex) {
                logger.error("Error in invoking WatchedUpdateListener", ex);
            }
        }
    }
}
