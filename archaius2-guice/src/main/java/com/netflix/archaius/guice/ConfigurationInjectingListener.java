package com.netflix.archaius.guice;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigLoader;
import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.annotations.ConfigurationSource;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.inject.LibrariesLayer;
import com.netflix.archaius.mapper.ConfigMapper;
import com.netflix.archaius.mapper.IoCContainer;

public class ConfigurationInjectingListener implements TypeListener, IoCContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInjectingListener.class);
    
    @Inject
    @LibrariesLayer
    private CompositeConfig librariesConfig;
    
    @Inject
    private Config rootConfig;
    
    @Inject
    private Injector injector;
    
    @Inject
    private ConfigMapper mapper;
    
    @Inject
    private ConfigLoader loader;
    
    @Inject
    private CascadeStrategy defaultStrategy;
    
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
                                             : defaultStrategy;
                                             
                    if (source != null) {
                        for (String resourceName : source.value()) {
                            try {
                                librariesConfig.addConfig(resourceName, loader.newLoader().withCascadeStrategy(strategy).load(resourceName));
                            } 
                            catch (ConfigException e) {
                                throw new ProvisionException("Unable to load configuration for " + resourceName + " at source " + injectee.getClass(), e);
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
                        mapper.mapConfig(injectee, rootConfig, ConfigurationInjectingListener.this);
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