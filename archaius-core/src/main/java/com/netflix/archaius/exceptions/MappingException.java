package com.netflix.archaius.exceptions;

public class MappingException extends Exception {
    public MappingException(Exception e) {
        super(e);
    }
    
    public MappingException(String message, Exception e) {
        super(message, e);
    }
}
