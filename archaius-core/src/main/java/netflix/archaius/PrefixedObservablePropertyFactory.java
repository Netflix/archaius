package netflix.archaius;

public class PrefixedObservablePropertyFactory implements ObservablePropertyFactory {

    private final String prefix;
    private final ObservablePropertyFactory delegate;
    
    public PrefixedObservablePropertyFactory(String prefix, ObservablePropertyFactory delegate) {
        this.prefix = !prefix.isEmpty() && !prefix.endsWith(".") 
                    ? prefix + "." 
                    : prefix;
        this.delegate = delegate;
    }
    
    @Override
    public ObservableProperty observeProperty(String propName) {
        return delegate.observeProperty(prefix + propName);
    }
}
