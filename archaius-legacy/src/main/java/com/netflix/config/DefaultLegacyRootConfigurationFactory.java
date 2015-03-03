package com.netflix.config;

import java.lang.reflect.Method;

import netflix.archaius.loaders.PropertiesConfigLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DefaultLegacyConfigurationManager.Builder;

public class DefaultLegacyRootConfigurationFactory implements LegacyRootConfigurationFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultLegacyRootConfigurationFactory.class);
    
    public static final String PROP_DEPLOYMENT_CONTEXT_FACTORY = "archaius.default.deploymentContext.factory";
    public static final String PROP_DEPLOYMENT_CONTEXT_CLASS   = "archaius.default.deploymentContext.class";
    public static final String PROP_CONFIGURATION_CLASS        = "archaius.default.configuration.class";
    public static final String PROP_CONFIGURATION_FACTORY      = "archaius.default.configuration.factory";
    public static final String PROP_DISABLE_DEFAULT_ENV_CONFIG = "archaius.dynamicProperty.disableEnvironmentConfig";
    public static final String PROP_DISABLE_DEFAULT_SYS_CONFIG = "archaius.dynamicProperty.disableSystemConfig";
    
    @Override
    public LegacyConfigurationManager create() throws Exception {
        Builder builder = DefaultLegacyConfigurationManager.builder()
                .withSystemProperties(!Boolean.parseBoolean(System.getProperty(PROP_DISABLE_DEFAULT_SYS_CONFIG, "false")))
                .withEnvironmentProperties(!Boolean.parseBoolean(System.getProperty(PROP_DISABLE_DEFAULT_ENV_CONFIG, "false")))
                .withConfigLoader(new PropertiesConfigLoader())
                ;
        
        String className = System.getProperty(PROP_DEPLOYMENT_CONTEXT_CLASS);
        if (className != null) {
            builder.withDeploymentContext((DeploymentContext) Class.forName(className).newInstance());
        }
        else {
            className = System.getProperty(PROP_DEPLOYMENT_CONTEXT_FACTORY);
            if (className != null) {
                Method m = Class.forName(className).getDeclaredMethod("getInstance", new Class[]{});
                m.setAccessible(true);
                builder.withDeploymentContext((DeploymentContext) m.invoke(null, new Object[]{}));
            } 
        }
        
        // TODO: Dynamic config URL

        return builder.build();
    }

}
