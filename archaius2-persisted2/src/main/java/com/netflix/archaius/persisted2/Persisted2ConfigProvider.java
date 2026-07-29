package com.netflix.archaius.persisted2;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
import com.netflix.archaius.instrumentation.AccessMonitorUtil;
import com.netflix.archaius.persisted2.loader.HTTPStreamLoader;

/**
 * Provider that sets up a Config that is a client to a Persisted2 service.
 * Once injected the Config will poll the service for updated on a configurable
 * interval.  Note that injection of this Config will block until the first
 * set of properties has been fetched from teh remote service.
 * 
 * The provider must be bound to a specific config layer within the override
 * hierarchy.
 * 
 * For example, Persisted2Config may boudn to the OverrideLayer like this,
 * <pre>
 *   final Persisted2ClientConfig config = new DefaultPersisted2ClientConfig()
 *          .withServiceUrl("http://persiste2serviceurl")
 *          .withQueryScope("env",    "test")
 *          .withQueryScope("region", "us-east-1")
 *          .withScope("env",    "test")
 *          .withScope("region", "us-east-1")
 *          .withScope("asg",    "app-v0001")
 *          .withPrioritizedScopes("env", "region", "stack", "serverId");
 *          
 *   Guice.createInjector(
 *      Modules
 *          .override(new ArchaiusModule())
 *          .with(new AbstractModule() {
 *              @Override
 *              protected void configure() {
 *                  bind(Persisted2ClientConfig.class).toInstance(config);
 *                  bind(Config.class).annotatedWith(OverrideLayer.class).toProvider(Persisted2ConfigProvider.class).in(Scopes.SINGLETON);
 *              }
 *          })
 *      )
 * </pre>
 * 
 * @author elandau
 *
 */
public class Persisted2ConfigProvider implements Provider<Config> {
    private final Logger LOG = LoggerFactory.getLogger(Persisted2ConfigProvider.class);
    
    private final Provider<Persisted2ClientConfig>  config;
    private final Optional<AccessMonitorUtil> accessMonitorUtilOptional;
    private volatile PollingDynamicConfig dynamicConfig;

    @Inject
    public Persisted2ConfigProvider(
            Provider<Persisted2ClientConfig> config,
            Optional<AccessMonitorUtil> accessMonitorUtilOptional) throws Exception {
        this.config = config;
        this.accessMonitorUtilOptional = accessMonitorUtilOptional;
    }
    
    public static String getFilterString(Map<String, Set<String>> scopes) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Set<String>> scope : scopes.entrySet()) {
            if (scope.getValue().isEmpty()) 
                continue;
            
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            
            sb.append("(");
            
            boolean first = true;
            for (String value : scope.getValue()) {
                if (!first) {
                    sb.append(" or ");
                }
                else {
                    first = false;
                }
                sb.append(scope.getKey());
                if (null == value) {
                    sb.append(" is null");
                }
                else if (value.isEmpty()) {
                    sb.append("=''");
                }
                else {
                    sb.append("='").append(value).append("'");
                }
            }
            
            sb.append(")");
        }
        return sb.toString();
    }
    
    @Override
    public Config get() {
        try {
            Persisted2ClientConfig clientConfig = config.get();
            LOG.info("Remote config : " + clientConfig);
            String url = new StringBuilder()
                .append(clientConfig.getServiceUrl())
                .append("?skipPropsWithExtraScopes=").append(clientConfig.getSkipPropsWithExtraScopes())
                .append("&filter=").append(URLEncoder.encode(getFilterString(clientConfig.getQueryScopes()), "UTF-8"))
                .toString();

            if (!clientConfig.isEnabled()) {
                return EmptyConfig.INSTANCE;
            }
            
            JsonPersistedV2Reader reader = JsonPersistedV2Reader.builder(new HTTPStreamLoader(new URL(url)))
                    .withPath("propertiesList")
                    .withScopes(clientConfig.getPrioritizedScopes())
                    .withPredicate(ScopePredicates.fromMap(clientConfig.getScopes()))
                    // If instrumentation flushing is enabled, we need to read the id fields as well to uniquely
                    // identify the property being used.
                    .withReadIdField(accessMonitorUtilOptional.isPresent())
                    .build();
            
            dynamicConfig =
                    new PollingDynamicConfig(
                            reader,
                            new FixedPollingStrategy(clientConfig.getRefreshRate(), TimeUnit.SECONDS),
                            accessMonitorUtilOptional.orElse(null));
            return dynamicConfig;
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (dynamicConfig != null) {
            dynamicConfig.shutdown();
        }
    }
}
