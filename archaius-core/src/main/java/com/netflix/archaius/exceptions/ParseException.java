package com.netflix.archaius.exceptions;

public class ParseException extends RuntimeException {
    public ParseException(Exception e) {
        super(e);
    }

    public ParseException(String message, Exception e) {
        super(message, e);
    }
}
