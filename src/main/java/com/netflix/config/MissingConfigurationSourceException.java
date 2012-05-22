package com.netflix.config;

public class MissingConfigurationSourceException extends RuntimeException {
    public MissingConfigurationSourceException(String message, Throwable e) {
        super(message, e);
    }
    public MissingConfigurationSourceException(String message) {
        super(message);
    }
    public MissingConfigurationSourceException(Throwable e) {
        super(e);
    }
}
