package com.netflix.archaius.guice;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProvidesMethodBinding;
import com.google.inject.spi.ProvisionListener;
import com.netflix.archaius.ConfigMapper;
import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigLoader;
import com.netflix.archaius.api.IoCContainer;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class ConfigurationInjectingListener implements ProvisionListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInjectingListener.class);
    
    @Inject
    private Config            config;
    
    @Inject
    private Injector          injector;
    
    @Inject
    private ConfigLoader      loader;
    
    @Inject
    private @LibrariesLayer   CompositeConfig   libraries;
    
    @com.google.inject.Inject(optional = true)
    private CascadeStrategy   cascadeStrategy;

    private Map<Binding<?>, ConfigurationSourceHolder> bindingToHolder = new ConcurrentHashMap<>();

    private ConfigMapper mapper = new ConfigMapper();

    @Inject
    public static void init(ConfigurationInjectingListener listener) {
        LOG.info("Initializing ConfigurationInjectingListener");
    }
    
    CascadeStrategy getCascadeStrategy() {
        return cascadeStrategy != null ? cascadeStrategy : NoCascadeStrategy.INSTANCE;
    }

    private static class ConfigurationSourceHolder {
        private final ConfigurationSource configurationSource;
        private final Configuration configuration;
        private final AtomicBoolean loaded = new AtomicBoolean();

        public ConfigurationSourceHolder(ConfigurationSource configurationSource, Configuration configuration) {
            this.configurationSource = configurationSource;
            this.configuration = configuration;
        }

        public void ifNotLoaded(BiConsumer<ConfigurationSource, Configuration> consumer) {
            if (loaded.compareAndSet(false, true) && (configurationSource != null || configuration != null)) {
                consumer.accept(configurationSource, configuration);
            }
        }
    }

    private ConfigurationSourceHolder getConfigurationSource(Binding<?> binding) {
        final ConfigurationSourceHolder holder = bindingToHolder.get(binding);
        if (holder != null) {
            return holder;
        }
        return bindingToHolder.computeIfAbsent(binding, this::createConfigurationSourceHolder);
    }

    private ConfigurationSourceHolder createConfigurationSourceHolder(Binding<?> binding) {
        return binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<Object, ConfigurationSourceHolder>() {
            @Override
            protected ConfigurationSourceHolder visitOther(Binding binding) {
                final Class<?> clazz = binding.getKey().getTypeLiteral().getRawType();
                return new ConfigurationSourceHolder(
                        clazz.getDeclaredAnnotation(ConfigurationSource.class),
                        clazz.getDeclaredAnnotation(Configuration.class)
                        );
            }

            @Override
            public ConfigurationSourceHolder visit(ProviderInstanceBinding providerInstanceBinding) {
                final Provider provider = providerInstanceBinding.getUserSuppliedProvider();
                if (provider instanceof ProvidesMethodBinding) {
                    final ProvidesMethodBinding providesMethodBinding = (ProvidesMethodBinding)provider;
                    ConfigurationSource configurationSource = providesMethodBinding.getMethod().getAnnotation(ConfigurationSource.class);
                    if (configurationSource != null) {
                        return new ConfigurationSourceHolder(
                                configurationSource,
                                null);
                    }
                }

                return super.visit(providerInstanceBinding);
            }
        });
    }

    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        getConfigurationSource(provision.getBinding()).ifNotLoaded((source, configAnnot) -> {
            Class<?> clazz = provision.getBinding().getKey().getTypeLiteral().getRawType();
            //
            // Configuration Loading
            //
            if (source != null) {
                if (injector == null) {
                    LOG.warn("Can't inject configuration into {} until ConfigurationInjectingListener has been initialized", clazz.getName());
                    return;
                }

                CascadeStrategy strategy = source.cascading() != ConfigurationSource.NullCascadeStrategy.class
                        ? injector.getInstance(source.cascading()) : getCascadeStrategy();

                List<String> sources = Arrays.asList(source.value());
                Collections.reverse(sources);
                for (String resourceName : sources) {
                    LOG.debug("Trying to loading configuration resource {}", resourceName);
                    try {
                        CompositeConfig loadedConfig = loader
                                .newLoader()
                                .withCascadeStrategy(strategy)
                                .load(resourceName);
                        libraries.addConfig(resourceName, loadedConfig);
                    } catch (ConfigException e) {
                        throw new ProvisionException("Unable to load configuration for " + resourceName, e);
                    }
                }
            }

            //
            // Configuration binding
            //
            if (configAnnot != null) {
                if (injector == null) {
                    LOG.warn("Can't inject configuration into {} until ConfigurationInjectingListener has been initialized", clazz.getName());
                    return;
                }

                try {
                    mapper.mapConfig(provision.provision(), config, new IoCContainer() {
                        @Override
                        public <S> S getInstance(String name, Class<S> type) {
                            return injector.getInstance(Key.get(type, Names.named(name)));
                        }
                    });
                }
                catch (Exception e) {
                    throw new ProvisionException("Unable to bind configuration to " + clazz.getName(), e);
                }
            }
        });
    }
}