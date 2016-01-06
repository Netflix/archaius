package com.netflix.archaius;

import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.readers.PropertiesConfigReader;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * @author Nikos Michalakis <nikos@netflix.com>
 */
public class DefaultConfigLoaderTest {

    @Test
    public void testBuildWithAllOptions() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");

        CompositeConfig application = new DefaultCompositeConfig();

        CompositeConfig config = DefaultCompositeConfig.builder()
                                                       .withConfig("app", application)
                                                       .withConfig("set", MapConfig.from(props))
                                                       .build();

        final StrInterpolator.Context mockContext = mock(StrInterpolator.Context.class);
        when(mockContext.resolve(anyString())).thenReturn("resolved");
        StrInterpolator mockStrInterpolator = mock(StrInterpolator.class);
        when(mockStrInterpolator.create(any(StrInterpolator.Lookup.class))).thenReturn(mockContext);

        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                                                        .withStrLookup(config)
                                                        .withStrInterpolator(mockStrInterpolator)
                                                        .withConfigReader(new PropertiesConfigReader())
                                                        .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
                                                        .build();

        application.replaceConfig("application", loader.newLoader().load("application"));
        Assert.assertTrue(config.getBoolean("application.loaded"));
    }
}
