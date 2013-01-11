package com.netflix.config.source;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConfigurationUpdateListener;
import com.netflix.config.ConfigurationUpdateResult;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.api.CuratorEvent;
import com.netflix.curator.framework.api.CuratorEventType;
import com.netflix.curator.framework.api.CuratorListener;
import com.netflix.curator.framework.recipes.cache.ChildData;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheListener;

public class ZooKeeperConfigurationSource implements WatchedConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfigurationSource.class);

    private final CuratorFramework client;
    private final String configRootPath;
    private final PathChildrenCache pathChildrenCache;

    private List<ConfigurationUpdateListener> listeners = new CopyOnWriteArrayList<ConfigurationUpdateListener>();

    public ZooKeeperConfigurationSource(CuratorFramework aClient, String aConfigRootPath) {
        this.client = aClient;
        this.configRootPath = aConfigRootPath;

        this.pathChildrenCache = new PathChildrenCache(aClient, aConfigRootPath, true);

        try {
            // synchronize config to get as close to a stable baseline as possible
            syncConfig(aClient, pathChildrenCache, aConfigRootPath);

            // create the child watcher
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                public void childEvent(CuratorFramework aClient, PathChildrenCacheEvent event)
                        throws Exception {
                    Type eventType = event.getType();
                    ChildData data = event.getData();
                    
                    String path = null; 
                    if (data != null) {
                        path = data.getPath();
                        
                        // scrub configRootPath out of the key name
                        String key = removeConfigRootPath(path);
    
                        byte[] value = data.getData();
                        String stringValue = new String(value, "UTF_8");
    
                        logger.debug("received update to pathName [{}], eventType [{}]");
                        logger.debug("key [{}], and value [{}]", key, stringValue);

                        // fire event to all listeners
                        Map<String, Object> added = null;
                        Map<String, Object> changed = null;
                        Map<String, Object> deleted = null;
                        if (eventType == Type.CHILD_ADDED) {
                            added = new HashMap<String, Object>(1);
                            added.put(key, stringValue);
                        } else if (eventType == Type.CHILD_UPDATED) {
                            changed = new HashMap<String, Object>(1);
                            changed.put(key, stringValue);
                        } else if (eventType == Type.CHILD_REMOVED) {
                            deleted = new HashMap<String, Object>(1);
                            deleted.put(key, stringValue);
                        }

                        ConfigurationUpdateResult result = ConfigurationUpdateResult.createIncremental(added,
                            changed, deleted);                        

                        fireEvent(result);
                    }
                }
            });

            pathChildrenCache.start();
        } catch (Exception exc) {
            logger.error("error initializing ZooKeeperWatchedConfigurationSource", exc);
        }
    }
    

    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        logger.debug("getCurrentData() retrieving current data.");

        syncConfig(client, pathChildrenCache, configRootPath);

        List<ChildData> children = pathChildrenCache.getCurrentData();
        Map<String, Object> all = new HashMap<String, Object>(children.size());
        for (ChildData child : children) {
            String path = child.getPath();
            String key = removeConfigRootPath(path);
            byte[] value = child.getData();

            all.put(key, new String(value, "UTF_8"));
        }

        logger.debug("getCurrentData() retrieved [{}] config elements.", children.size());

        return all;
    }

    @Override
    public void addConfigurationUpdateListener(ConfigurationUpdateListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeConfigurationUpdateListener(ConfigurationUpdateListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    /**
     * Sync the ZK nodes and get a snapshot of the latest data.<BR>
     * 
     * The correctness of this data is only as good as the {@link CuratorFramework#sync(String, Object)} and the
     * {@link PathChildrenCache#rebuild()} methods.
     */
    public static synchronized void syncConfig(CuratorFramework client, PathChildrenCache pathChildrenCache, String rootPath) throws Exception {
        logger.info("syncConfig: started client sync and pathChildrenCache rebuild at ZK root path [{}]", rootPath);

        // TODO:  use BlockingQueue and make sure to append a Refresh message at the end to catch anything that creeped up while initializing (see
        final CountDownLatch syncLatch = new CountDownLatch(1);

        client.getCuratorListenable().addListener(new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                if (event.getType() == CuratorEventType.SYNC) {
                    syncLatch.countDown();
                }
            }
        });

        // async call turned into blocking call by using the syncLatch
        client.sync(rootPath, syncLatch);
        try {
            syncLatch.await();
        } catch (final InterruptedException exc) {
            logger.error("interrupted while waiting on syncLatch [{}]", syncLatch, exc);
            throw new Exception("interrupted while waiting on syncLatch [" + syncLatch + "]", exc);
        }

        // rebuild a snapshot of the data without notifying listeners (blocking call)
        try {
            pathChildrenCache.rebuild();
        } catch (final Exception exc) {
            logger.error("exception while rebuilding the pathChildrenCache [{}]", pathChildrenCache, exc);
            throw new Exception("exception while rebuilding the pathChildrenCache [" + pathChildrenCache
                    + "]", exc);
        }

        logger.info("finished sync and pathChildrenCache at config root path [{}]", rootPath);
    }

    protected void fireEvent(ConfigurationUpdateResult result) {
        for (ConfigurationUpdateListener l : listeners) {
            try {
                l.updateConfiguration(result);
            } catch (Throwable ex) {
                logger.error("Error in invoking ConfigurationUpdateListener", ex);
            }
        }
    }

    /**
     * This is used to convert a configuration nodePath into a key
     * 
     * @param nodePath
     * 
     * @return key (nodePath less the config root path)
     */
    protected String removeConfigRootPath(String nodePath) {
        return nodePath.replace(configRootPath + "/", "");
    }  
    
    //@VisibleForTesting
    synchronized void setZkProperty(String key, String value) throws Exception {
        final CountDownLatch updateLatch = new CountDownLatch(1);
        
        PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
            public void childEvent(final CuratorFramework client, final PathChildrenCacheEvent event) throws Exception {
                ZooKeeperConfigurationSource.syncConfig(client, pathChildrenCache, configRootPath);
                if (updateLatch != null && updateLatch.getCount() > 0) { 
                    logger.trace("flipping latch after event [{}] with [{}] count remaining.", event, updateLatch.getCount());
                    updateLatch.countDown();
                }
            }
        };
        
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);        

        byte[] data = value.getBytes(Charset.forName("UTF-8"));
        try {
            client.create().creatingParentsIfNeeded().forPath(configRootPath + "/" + key, data);
        } catch (NodeExistsException exc) {
            client.setData().forPath(configRootPath + "/" + key, data);              
        } catch (Exception exc) {
            updateLatch.countDown();
        }
        updateLatch.await();

        pathChildrenCache.getListenable().removeListener(pathChildrenCacheListener);
    }

    //@VisibleForTesting
    synchronized String getZkProperty(String key) throws Exception {
        byte[] bytes = client.getData().forPath(configRootPath + "/" + key);

        return new String(bytes, "UTF_8");
    }
    
    //@VisibleForTesting
    synchronized void deleteZkProperty(String key) throws Exception {
        final CountDownLatch updateLatch = new CountDownLatch(1);        

        PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
            public void childEvent(final CuratorFramework client, final PathChildrenCacheEvent event) throws Exception {
                ZooKeeperConfigurationSource.syncConfig(client, pathChildrenCache, configRootPath);
                if (updateLatch != null && updateLatch.getCount() > 0) { 
                    logger.trace("flipping latch after event [{}] with [{}] count remaining.", event, updateLatch.getCount());
                    updateLatch.countDown();
                }
            }
        };
        
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);        
        try {
            client.delete().forPath(configRootPath + "/" + key);        
        } catch (Exception exc) {
            updateLatch.countDown();
        }

        updateLatch.await();        
        pathChildrenCache.getListenable().removeListener(pathChildrenCacheListener);
    }
}
