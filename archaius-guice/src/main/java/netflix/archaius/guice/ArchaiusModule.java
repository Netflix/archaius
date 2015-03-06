package netflix.archaius.guice;

import javax.inject.Inject;

import netflix.archaius.AppConfig;
import netflix.archaius.CascadeStrategy;
import netflix.archaius.Config;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.mapper.ConfigBinder;
import netflix.archaius.mapper.DefaultConfigBinder;
import netflix.archaius.mapper.annotations.Configuration;
import netflix.archaius.mapper.annotations.ConfigurationSource;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class ArchaiusModule extends AbstractModule {
    public static class ConfigurationInjectingListener implements TypeListener {
        @Inject
        private AppConfig appConfig;
        
        @Inject
        private Injector injector;
        
        @Inject
        private ConfigBinder binder;
        
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
                            binder.bindConfig(injectee);
                        }
                        catch (Exception e) {
                            throw new ProvisionException("Unable to bind configuration to " + injectee.getClass());
                        }
                    }
                });
            }        
        }
    }
    
    @Override
    protected void configure() {
        ConfigurationInjectingListener listener = new ConfigurationInjectingListener();
        requestInjection(listener);
        
        bindListener(Matchers.any(), listener);
    }
    
    @Provides
    public ConfigBinder get(Config config) {
        return new DefaultConfigBinder(config);
    }
}
