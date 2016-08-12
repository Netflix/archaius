package com.netflix.archaius.test;

public class TestConfigException extends RuntimeException {

    private static final long serialVersionUID = 8598260463522600221L;

    public TestConfigException(String msg) {
        super(msg);
    }
    
    public TestConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
