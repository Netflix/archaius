package com.netflix.archaius.persisted2;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
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
    private final String              url;
    private final Persisted2ClientConfig config;
    private volatile PollingDynamicConfig dynamicConfig;
    
    @Inject
    public Persisted2ConfigProvider(Persisted2ClientConfig config) throws Exception {
        this.url = new StringBuilder()
            .append(config.getServiceUrl())
            .append("?skipPropsWithExtraScopes=").append(config.getSkipPropsWithExtraScopes())
            .append("&filter=").append(URLEncoder.encode(getFilterString(config.getQueryScopes()), "UTF-8"))
            .toString();
        this.config = config;
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
        if (!config.isEnabled()) {
            return EmptyConfig.INSTANCE;
        }
        
        JsonPersistedV2Reader reader;
        try {
            reader = JsonPersistedV2Reader.builder(new HTTPStreamLoader(new URL(url)))
                .withPath("propertiesList")
                .withScopes(config.getPrioritizedScopes())
                .withPredicate(ScopePredicates.fromMap(config.getScopes()))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up reader", e);
        }
        
        return dynamicConfig = new PollingDynamicConfig(reader, new FixedPollingStrategy(config.getRefreshRate(), TimeUnit.SECONDS));
    }
    
    @PreDestroy
    public void shutdown() {
        if (dynamicConfig != null) {
            dynamicConfig.shutdown();
        }
    }
}
