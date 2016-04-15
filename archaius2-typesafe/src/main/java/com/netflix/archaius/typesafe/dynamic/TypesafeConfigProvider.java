package com.netflix.archaius.typesafe.dynamic;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

/**
 * Provider that sets up a Config which is backed up by a typesafe loader.
 * Once injected the Config will poll the service for updated on a configurable
 * interval.  Note that injection of this Config will block until the first
 * set of properties has been fetched from the typesafe service.
 *
 * The provider must be bound to a specific config layer within the override
 * hierarchy.
 *
 * For example, TypesafeClientConfig may bound to the OverrideLayer like this,
 * <pre>
 *   final TypesafeClientConfig config = new DefaultTypesafeClientConfig.Builder()
 *          .withConfigFilePath("http://example.com/typesafe/reference.conf") // or file:///tmp/typesafe/reference.conf
 *          .withRefreshIntervalMs(60 * 1000)
 *          .build();
 *
 *   Guice.createInjector(
 *      Modules
 *          .override(new ArchaiusModule())
 *          .with(new AbstractModule() {
 *              @Override
 *              protected void configureArchaius() {
 *                  bind(TypesafeClientConfig.class).toInstance(config);
 *                  bind(Config.class).annotatedWith(OverrideLayer.class).toProvider(TypesafeConfigProvider.class).in(Scopes.SINGLETON);
 *                  // or bind to RemoteLayer
 *                  // bind(Config.class).annotatedWith(RemoteLayer.class).toProvider(TypesafeConfigProvider.class).in(Scopes.SINGLETON);
 *              }
 *          })
 *      )
 * </pre>
 *
 */
public class TypesafeConfigProvider implements Provider<Config> {
    private final Logger LOG = LoggerFactory.getLogger(TypesafeConfigProvider.class);
    
    private volatile PollingDynamicConfig dynamicConfig;
    private TypesafeClientConfig clientConfig;

    @Inject
    public TypesafeConfigProvider(TypesafeClientConfig clientConfig) throws Exception {
        this.clientConfig = clientConfig;
    }
    
    public Config get() {
        if (!clientConfig.isEnabled()) {
            LOG.info("Typesafe config is not enabled.");
            return EmptyConfig.INSTANCE;
        }

        try {
            return dynamicConfig = new PollingDynamicConfig(new TypesafeConfigLoader(clientConfig.getTypesafeConfigSupplier()),
                    new FixedPollingStrategy(clientConfig.getRefreshRateMs(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LOG.error("Unable to initialize the dynamic typesafe config", e);
            throw new RuntimeException(e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down TypesafeConfigProvider: {}.", toString());
        if (dynamicConfig != null) {
            dynamicConfig.shutdown();
        }
    }
}