package com.netflix.archaius.exceptions;

public class ConverterNotFoundException extends RuntimeException {
    public ConverterNotFoundException(String message) {
        super(message);
    }
    public ConverterNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
