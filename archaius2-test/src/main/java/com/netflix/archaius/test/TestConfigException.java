package com.netflix.archaius.test;

public class TestConfigException extends RuntimeException {

    public TestConfigException(String msg) {
        super(msg);
    }
    
    public TestConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
