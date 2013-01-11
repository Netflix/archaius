package com.netflix.config.validation;

public interface PropertyChangeValidator {
    
    public void validate(String newValue) throws ValidationException;
}
