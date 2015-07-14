package com.netflix.archaius.guice;

import java.lang.annotation.Annotation;
import java.util.Properties;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.archaius.Config;
import com.netflix.archaius.config.MapConfig;

/**
 * Utility class for adding config seeders to a Guice module
 * 
 * @author elandau
 *
 */
public abstract class ConfigSeeders {
    /**
     * Add a seeder for Properties to a specific annotated layer.
     * 
     * For example, to seed the RuntimeLayer,
     * 
     * <pre>
     * {@code 
     * public class MyModule extends AbstractModule() {
     *     protected void configure() {
     *         Properties props = new Properties();
     *         props.setProperty("name", "value");
     *         ConfigSeeders.bind(binder(), props, RuntimeLayer.class);
     *     }
     * }
     * </pre>
     * 
     * @param binder
     * @param props
     * @param annot
     */
    public static void bind(Binder binder, Properties props, Class<? extends Annotation> annot) {
        bind(binder, MapConfig.from(props), annot);
    }
    
    /**
     * Add a seeder for Properties to a specific annotated layer.
     * 
     * For example, to seed the RuntimeLayer,
     * 
     * <pre>
     * {@code 
     * public class MyModule extends AbstractModule() {
     *     protected void configure() {
     *         ConfigSeeders.bind(binder(), 
     *                  MapConfig.builder()
     *                      .put("name", "value")
     *                      .build(), 
     *                  RuntimeLayer.class);
     *     }
     * }
     * </pre>
     * 
     * @param binder
     * @param props
     * @param annot
     */    
    public static void bind(Binder binder, final Config config, Class<? extends Annotation> annot) {
        Multibinder.newSetBinder(binder, ConfigSeeder.class, annot)
                    .addBinding().toInstance(new ConfigSeeder() {
                        public Config get(Config mainConfig) {
                            return config;
                        }
                     });
        
    }

    public static LinkedBindingBuilder<ConfigSeeder> bind(Binder binder, Class<? extends Annotation> annot) {
        return Multibinder.newSetBinder(binder, ConfigSeeder.class, annot)
            .addBinding();
    }

    public static ConfigSeeder from(final Properties prop) {
        return from(MapConfig.from(prop));
    }
    
    public static ConfigSeeder from(final Config config) {
        return new ConfigSeeder() {
            @Override
            public Config get(Config rootConfig) throws Exception {
                return config;
            }
        };
    }
}
