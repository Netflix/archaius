package com.netflix.archaius.bridge;

import com.google.inject.AbstractModule;
import com.netflix.config.DeploymentContext;

/**
 * Module with bindings to bridge the legacy static Archaius1 ConfigurationManager API with the new
 * the Archaius2 Config guice bindings.  Configuration loaded into either library will be visible
 * to the other.
 * 
 * To install,
 * <pre>
 * {@code
 *      Guice.createInjector(new ArchaiusModule(), new StaticArchaiusBridgeModule());
 * }
 * </pre>
 * 
 * When running multiple unit tests make sure to add the following @Before method to your JUnit classes
 * 
 * <pre>
 * {@code
 *     @Before
 *     public void before() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
 *         StaticAbstractConfiguration.reset();
 *         StaticDeploymentContext.reset();
 *     }
 * }
 * </pre>
 * 
 * @author elandau
 *
 */
public class StaticArchaiusBridgeModule extends AbstractModule {
    static {
        System.setProperty("archaius.default.configuration.class",      StaticAbstractConfiguration.class.getName());
        System.setProperty("archaius.default.deploymentContext.class",  StaticDeploymentContext.class.getName());
    }
    
    @Override
    protected void configure() {
        requestStaticInjection(StaticAbstractConfiguration.class);
        requestStaticInjection(StaticDeploymentContext.class);
        bind(DeploymentContext.class).to(ConfigBasedDeploymentContext.class);
    }
}
