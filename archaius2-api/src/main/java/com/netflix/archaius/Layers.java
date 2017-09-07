package com.netflix.archaius;

import com.netflix.archaius.api.Layer;

public final class Layers {
    public static Layer TEST        = Layer.of("test",        100);
    public static Layer RUNTIME     = Layer.of("runtime",     200);
    public static Layer SYSTEM      = Layer.of("system",      300);
    public static Layer ENVIRONMENT = Layer.of("environment", 400);
    public static Layer REMOTE      = Layer.of("remote",      500);
    public static Layer APPLICATION_OVERRIDE = Layer.of("application-override", 600);
    public static Layer APPLICATION = Layer.of("application", 700);
    public static Layer LIBRARY     = Layer.of("library",     800);
    public static Layer DEFAULT     = Layer.of("default",     900);
    
    private Layers() {
    }
}