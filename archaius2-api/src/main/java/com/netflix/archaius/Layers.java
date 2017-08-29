package com.netflix.archaius;

import com.netflix.archaius.api.Layer;

public final class Layers {
    public static Layer TEST        = Layer.of("test",        100, false);
    public static Layer RUNTIME     = Layer.of("runtime",     200, false);
    public static Layer SYSTEM      = Layer.of("system",      300, false);
    public static Layer ENVIRONMENT = Layer.of("environment", 400, false);
    public static Layer REMOTE      = Layer.of("remote",      500, false);
    public static Layer APPLICATION_OVERRIDE = Layer.of("application-override", 600, true);
    public static Layer APPLICATION = Layer.of("application", 700, true);
    public static Layer LIBRARY     = Layer.of("library",     800, false);
    public static Layer DEFAULT     = Layer.of("default",     900, false);
    
    private Layers() {
    }
}