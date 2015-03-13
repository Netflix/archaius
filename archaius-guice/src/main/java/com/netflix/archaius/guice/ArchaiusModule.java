/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.guice;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

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
import com.netflix.archaius.AppConfig;
import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.Config;
import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.mapper.ConfigMapper;
import com.netflix.archaius.mapper.DefaultConfigMapper;
import com.netflix.archaius.mapper.IoCContainer;
import com.netflix.archaius.mapper.annotations.Configuration;
import com.netflix.archaius.mapper.annotations.ConfigurationSource;

public class ArchaiusModule extends AbstractModule {
    
    public static class ConfigProvider<T> implements Provider<T> {
        private Class<T> type;
        
        @Inject
        ConfigMapper mapper;
        
        @Inject
        PropertyFactory factory;
        
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
    private PropertyFactory createObservablePropertyFactory(AppConfig config) {
        return config;
    }

}
