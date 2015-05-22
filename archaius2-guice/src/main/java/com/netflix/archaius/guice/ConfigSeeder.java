package com.netflix.archaius.guice;

import com.netflix.archaius.Config;

/**
 * API used to seed a Config into a specific layer of the root CompositeConfig.
 * Multiple ConfigSeeders may be bound for each layer and are invoked when the
 * main Config is constructed.
 * 
 * For example, the following code
 * <pre>
 * Multibinder.newSetBinder(binder(), ConfigSeeder, RuntimeLayer.class)
 *            .addBinding().toInstance(new ConfigSeeder() {
 *                public Config get(Config mainConfig) {
 *                    return MapConfig.from(props));
 *                }
 *             });
 * </pre>
 * will load properties from 'prop' into the RuntimeLayer.
 * 
 * Note that {@link ConfigSeeder#get(Config)} receives the root configuration
 * as a parameter.  This enables the seeder to set properties based on the current
 * state of the root composite config.  Note that ConfigSeeders are invoked in the
 * order in which they are bound.
 * 
 * @author elandau
 *
 */
public interface ConfigSeeder {
    /**
     * Return a Config to load into the layer indicated by the binding.  
     * 
     * @param rootConfig    Current state of the root Config to be used for any 
     *                      property loading or interpoltion needed by the ConfigResolver
     * @return
     * @throws Exception
     */
    public Config get(Config rootConfig) throws Exception;
}
