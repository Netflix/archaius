package com.netflix.config;

public class DynamicURLConfiguration extends DynamicConfiguration {
        
    public DynamicURLConfiguration() {
        super(new URLConfigurationSource(), new FixedDelayPollingScheduler());
    }

    public DynamicURLConfiguration(int initialDelayMillis, int delayMillis, boolean ignoreDeletesFromSource, 
            String... urls) {
        super(new URLConfigurationSource(urls),
                new FixedDelayPollingScheduler(initialDelayMillis, delayMillis, ignoreDeletesFromSource));
    }    
}
