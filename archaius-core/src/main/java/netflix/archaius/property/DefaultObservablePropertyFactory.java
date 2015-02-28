package netflix.archaius.property;

import netflix.archaius.Config;
import netflix.archaius.ObservableProperty;
import netflix.archaius.ObservablePropertyFactory;

public class DefaultObservablePropertyFactory implements ObservablePropertyFactory {

    private Config config;

    public DefaultObservablePropertyFactory(Config config) {
        this.config = config;
    }
    
    @Override
    public ObservableProperty create(String key) {
        return new DefaultObservableProperty(key, config);
    }

}
