package com.netflix.config.util;

// Interface which surfaces instrumentation-related endpoints for configurations
public interface InstrumentationAware {
    Object getPropertyUninstrumented(String key);
}
