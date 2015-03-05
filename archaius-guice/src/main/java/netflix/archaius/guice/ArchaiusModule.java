package netflix.archaius.guice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

import netflix.archaius.AppConfig;
import netflix.archaius.Config;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.guice.annotations.Configuration;
import netflix.archaius.guice.annotations.ConfigurationSource;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class ArchaiusModule extends AbstractModule {
    public static class ConfigurationInjectingListener implements TypeListener {
        @Inject
        private Config config;
        
        @Inject
        private AppConfig appConfig;
        
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
                        if (source != null) {
                            for (String value : source.value()) {
                                try {
                                    appConfig.addConfigFirst(appConfig.newLoader().load(value));
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
                        Configuration configAnnot = injectee.getClass().getAnnotation(Configuration.class);
                        if (configAnnot != null) {
                            String prefix = config.getStrInterpolator().resolve(configAnnot.prefix()).toString();
                            if (!prefix.isEmpty() || !prefix.endsWith("."))
                                prefix += ".";
                            
                            for (Field field : injectee.getClass().getDeclaredFields()) {
                                String name = field.getName();
                                Class<?> type = field.getType();
                                Object value = config.get(type, prefix + name, null);
                                if (value != null) {
                                    try {
                                        field.setAccessible(true);
                                        field.set(injectee, value);
                                    } catch (Exception e) {
                                        throw new ProvisionException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                                    }
                                }
                            }
                            
                            for (Method method : injectee.getClass().getDeclaredMethods()) {
                                // Only support methods with one parameter 
                                //  Ex.  setTimeout(int timeout);
                                if (method.getParameterTypes().length != 1) {
                                    continue;
                                }
                                
                                // Extract field name from method name
                                //  Ex.  setTimeout => timeout
                                String name = method.getName();
                                if (name.startsWith("set") && name.length() > 3) {
                                    name = name.substring(3,4).toLowerCase() + name.substring(4);
                                }
                                // Or from builder
                                //  Ex.  withTimeout => timeout
                                else if (name.startsWith("with") && name.length() > 4) {
                                    name = name.substring(4,1).toLowerCase() + name.substring(5);
                                }
                                else {
                                    continue;
                                }

                                method.setAccessible(true);
                                Class<?> type = method.getParameterTypes()[0];
                                Object value = config.get(type, prefix + name, null);
                                if (value != null) {
                                    try {
                                        method.invoke(injectee, value);
                                    } catch (Exception e) {
                                        throw new ProvisionException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                                    }
                                }
                            }
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
}
