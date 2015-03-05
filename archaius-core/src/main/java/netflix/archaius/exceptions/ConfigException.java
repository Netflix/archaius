package netflix.archaius.exceptions;

public class ConfigException extends Exception {
    public ConfigException(String message) {
        super(message);
    }
    
    public ConfigException(String message, Throwable t) {
        super(message, t);
    }
}
