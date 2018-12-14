package com.netflix.config.source;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;
import static com.netflix.config.WatchedUpdateResult.createIncremental;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the dynamic {@link WatchedConfigurationSource} for Kubernetes ConfigMaps
 *
 * This implementation requires a label to be set on the ConfigMap, e.g.
 * apiVersion: v1
 * kind: ConfigMap
 * metadata:
 *   name: archaius-config
 *   namespace: default
 *   labels:
 *       app: archaius-config
 * data:
 *   property.name.example.1: this is conf value 1
 *   property.name.example.2: this is conf value 2
 *
 * @author luckyswede
 */
public class K8sConfigurationSource implements WatchedConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(K8sConfigurationSource.class);
    private final Map<String, Object> valueCache = Maps.newConcurrentMap();
    private final List<WatchedUpdateListener> listeners = new CopyOnWriteArrayList<WatchedUpdateListener>();
    private final String labelKey;
    private final String labelValue;
    private boolean running = false;
    private CountDownLatch closeLatch;
    protected ConfigMapWatcher configMapWatcher = new ConfigMapWatcher();

    /**
     * Will listen for config changes on the ConfigMap with the provided label in the same namespace as the application.
     * @param labelKey label key for the ConfigMap containing Dynamic properties
     * @param labelValue label value for the ConfigMap containing Dynamic properties
     */
    public K8sConfigurationSource(String labelKey, String labelValue) {
        this.labelKey = labelKey;
        this.labelValue = labelValue;
    }

    /**
     * Starts a background thread that listens to changes in the ConfigMap defined by the label provided in the
     * constructor.
     */
    public void start() {
        running = true;
        closeLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Config config = Config.autoConfigure(null);

                logger.info("Started");
                while (running) {
                    try (final KubernetesClient client = new DefaultKubernetesClient(config)) {
                        logger.debug("Running in namespace " + client.getNamespace());
                        try (Watch watch =
                                     client.configMaps()
                                        .inNamespace(client.getNamespace())
                                        .withLabel(labelKey, labelValue)
                                        .watch(configMapWatcher)
                        ) {
                            // Trigger a close on the watcher in X seconds when exiting the try clause
                            closeLatch.await(60, TimeUnit.SECONDS);
                        }
                        catch (KubernetesClientException | InterruptedException e) {
                            logger.warn("Could not watch resources", e);
                        }
                    }
                    catch (Exception e) {
                        logger.warn("Could not create API client", e);
                    }
                    try {
                        Thread.sleep(1000l);
                    }
                    catch (InterruptedException e) {}
                }
                logger.info("Stopped");
            }
        }).start();
    }

    /**
     * Stop the background thread.
     */
    public void stop() {
        running = false;
        closeLatch.countDown();
    }

    private void updateConfiguration(WatchedUpdateResult result) {
        for (WatchedUpdateListener l : listeners) {
            try {
                l.updateConfiguration(result);
            }
            catch (Throwable ex) {
                logger.error("Error in invoking WatchedUpdateListener", ex);
            }
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

    class ConfigMapWatcher implements Watcher<ConfigMap> {
        @Override
        public void eventReceived(Action action, ConfigMap resource) {
            // In both sets, check for updated values
            Set<String> intersection = new HashSet<>(resource.getData().keySet());
            intersection.retainAll(valueCache.keySet());
            for (String key : intersection) {
                if (valueCache.get(key) == null || !valueCache.get(key).equals(resource.getData().get(key))) {
                    logger.debug("Updated: " + key);
                    valueCache.put(key, resource.getData().get(key));
                    updateConfiguration(createIncremental(null, ImmutableMap.<String, Object>of(key, resource.getData().get(key)), null));
                }
            }
            // Added
            Set<String> added = new HashSet<>(resource.getData().keySet());
            added.removeAll(valueCache.keySet());
            for (String key : added) {
                logger.debug("Added: " + key);
                valueCache.put(key, resource.getData().get(key));
                updateConfiguration(createIncremental(ImmutableMap.<String, Object>of(key, resource.getData().get(key)), null, null));
            }
            // Deleted
            Set<String> deleted = new HashSet<>(valueCache.keySet());
            deleted.removeAll(resource.getData().keySet());
            for (String key : deleted) {
                logger.debug("Deleted: " + key);
                valueCache.remove(key);
                updateConfiguration(createIncremental(null, null, ImmutableMap.<String, Object>of(key, "")));
            }
        }

        @Override
        public void onClose(KubernetesClientException cause) {
            logger.debug("Watcher onClose");
            if (cause != null) {
                logger.warn("Got error", cause);
                closeLatch.countDown();
            }
        }
    }
}
