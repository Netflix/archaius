package netflix.archaius.exceptions;

public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable t) {
        super(message, t);
    }
}
