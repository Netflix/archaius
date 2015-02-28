package netflix.archaius;

/**
 * Factory for creating an ObservablePropert making it possible to provide custom
 * ObservableProperty implementations that use different locking and concurrency
 * semantics.
 * 
 * @author elandau
 */
public interface ObservablePropertyFactory {
    /**
     * Create an observable for the property name.  
     */
    public ObservableProperty create(String propName);
}
