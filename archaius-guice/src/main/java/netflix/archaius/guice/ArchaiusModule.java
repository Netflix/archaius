package netflix.archaius.guice;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import netflix.archaius.AppConfig;
import netflix.archaius.CascadeStrategy;
import netflix.archaius.Config;
import netflix.archaius.DefaultAppConfig;
import netflix.archaius.ObservablePropertyFactory;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.mapper.ConfigMapper;
import netflix.archaius.mapper.DefaultConfigMapper;
import netflix.archaius.mapper.IoCContainer;
import netflix.archaius.mapper.annotations.Configuration;
import netflix.archaius.mapper.annotations.ConfigurationSource;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.google.inject.util.Providers;

public class ArchaiusModule extends AbstractModule {
    
    public static class ConfigProvider<T> implements Provider<T> {
        private Class<T> type;
        
        @Inject
        ConfigMapper mapper;
        
        @Inject
        ObservablePropertyFactory factory;
        
        public ConfigProvider(Class<T> type) {
            this.type = type;
        }

        @Override
        public T get() {
            return mapper.newProxy(type, factory);
        }
    }
    
    public static <T> AbstractModule forProxy(final Class<T> proxy) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Provider<T> provider = new ConfigProvider<T>(proxy);
                requestInjection(provider);
                bind(proxy).toProvider(Providers.guicify(provider));
            }
        };
    }
    
    public static class ConfigurationInjectingListener implements TypeListener, IoCContainer {
        @Inject
        private AppConfig appConfig;
        
        @Inject
        private Injector injector;
        
        @Inject
        private ConfigMapper mapper;
        
        @Override
        public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> encounter) {
            Class<?> clazz = typeLiteral.getRawType();
            
            //
            // Configuration Loading
            //
            ConfigurationSource source = clazz.getAnnotation(ConfigurationSource.class);
            if (source != null) {
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(I injectee) {
                        ConfigurationSource source = injectee.getClass().getAnnotation(ConfigurationSource.class);
                        CascadeStrategy strategy = source.cascading() != ConfigurationSource.NullCascadeStrategy.class
                                                 ? injector.getInstance(source.cascading()) 
                                                 : null;
                        if (source != null) {
                            for (String value : source.value()) {
                                try {
                                    appConfig.addConfigFirst(appConfig.newLoader().withCascadeStrategy(strategy).load(value));
                                } catch (ConfigException e) {
                                    throw new ProvisionException("Unable to load configuration for " + value + " at source " + injectee.getClass(), e);
                                }
                            }
                        }
                    }
                });
            }
            
            //
            // Configuration binding
            //
            Configuration configAnnot = clazz.getAnnotation(Configuration.class);
            if (configAnnot != null) {
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(I injectee) {
                        try {
                            mapper.mapConfig(injectee, appConfig, ConfigurationInjectingListener.this);
                        }
                        catch (Exception e) {
                            throw new ProvisionException("Unable to bind configuration to " + injectee.getClass(), e);
                        }
                    }
                });
            }        
        }

        @Override
        public <T> T getInstance(String name, Class<T> type) {
            return injector.getInstance(Key.get(type, Names.named(name)));
        }
    }
    
    @Override
    protected void configure() {
        ConfigurationInjectingListener listener = new ConfigurationInjectingListener();
        requestInjection(listener);
        
        bindListener(Matchers.any(), listener);
    }
    
    @Provides
    @Singleton
    protected ConfigMapper createConfigMapper() {
        return new DefaultConfigMapper();
    }
    
    @Provides
    @Singleton
    protected AppConfig createAppConfig() {
        return DefaultAppConfig.builder().build();
    }
    
    @Provides
    @Singleton
    private Config createConfig(AppConfig config) {
        return config;
    }

    @Provides
    @Singleton
    private ObservablePropertyFactory createObservablePropertyFactory(AppConfig config) {
        return config;
    }

}
