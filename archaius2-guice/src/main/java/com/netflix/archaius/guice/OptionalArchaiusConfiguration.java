package com.netflix.archaius.guice;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.netflix.archaius.CascadeStrategy;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.Decoder;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.cascade.NoCascadeStrategy;
import com.netflix.archaius.inject.ApplicationLayer;
import com.netflix.archaius.inject.ApplicationOverrideLayer;
import com.netflix.archaius.inject.DefaultsLayer;
import com.netflix.archaius.inject.LibrariesLayer;
import com.netflix.archaius.inject.RemoteLayer;
import com.netflix.archaius.inject.RuntimeLayer;

@Singleton
public class OptionalArchaiusConfiguration implements ArchaiusConfiguration {

    @Inject(optional=true)
    @RuntimeLayer
    Set<ConfigSeeder> runtimeLayerSeeders;
    
    @Inject(optional=true)
    @RemoteLayer
    Set<ConfigSeeder> remoteLayerSeeders;
    
    @Inject(optional=true)
    @DefaultsLayer
    Set<ConfigSeeder> defaultsLayerSeeders;
    
    @Inject(optional=true)
    Set<ConfigListener> configListeners;
    
    @Inject(optional=true)
    @ApplicationLayer
    String configurationName;
    
    @Inject(optional=true)
    CascadeStrategy cascadeStrategy;
    
    @Inject(optional=true)
    Decoder decoder;
    
    @Inject(optional=true)
    @LibrariesLayer
    Map<String, Config> libraryOverrides;
    
    @Inject(optional=true)
    @ApplicationOverrideLayer
    Config applicationOverrides;
    
    @Override
    public Set<ConfigSeeder> getRuntimeLayerSeeders() {
        return runtimeLayerSeeders != null ? runtimeLayerSeeders : Collections.<ConfigSeeder>emptySet();
    }

    @Override
    public Set<ConfigSeeder> getRemoteLayerSeeders() {
        return remoteLayerSeeders != null ? remoteLayerSeeders : Collections.<ConfigSeeder>emptySet();
    }

    @Override
    public Set<ConfigSeeder> getDefaultsLayerSeeders() {
        return defaultsLayerSeeders != null ? defaultsLayerSeeders : Collections.<ConfigSeeder>emptySet();
    }

    @Override
    public String getConfigName() {
        return configurationName != null ? configurationName : "application";
    }

    @Override
    public CascadeStrategy getCascadeStrategy() {
        return cascadeStrategy != null ? cascadeStrategy : NoCascadeStrategy.INSTANCE;
    }

    @Override
    public Decoder getDecoder() {
        return decoder != null ? decoder : DefaultDecoder.INSTANCE;
    }

    @Override
    public Set<ConfigListener> getConfigListeners() {
        return configListeners != null ? configListeners : Collections.<ConfigListener>emptySet();
    }

    @Override
    public Map<String, Config> getLibraryOverrides() {
        return libraryOverrides != null ? libraryOverrides : Collections.<String, Config>emptyMap();
    }

    @Override
    public Config getApplicationOverride() {
        return applicationOverrides;
    }
}
