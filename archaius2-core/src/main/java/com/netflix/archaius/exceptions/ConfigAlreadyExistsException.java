package com.netflix.archaius.exceptions;

import com.netflix.archaius.api.exceptions.ConfigException;

public class ConfigAlreadyExistsException extends ConfigException {
    public ConfigAlreadyExistsException(String message) {
        super(message);
    }

}
