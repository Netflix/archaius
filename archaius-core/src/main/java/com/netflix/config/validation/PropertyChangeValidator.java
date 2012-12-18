package com.netflix.config.validation;

public interface PropertyChangeValidator {
    
    public boolean validate(String newValue);
}
