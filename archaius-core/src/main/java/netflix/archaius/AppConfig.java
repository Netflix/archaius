package netflix.archaius;

import netflix.archaius.config.CompositeConfig;
import netflix.archaius.config.SettableConfig;

public interface AppConfig extends ObservablePropertyFactory, SettableConfig, CompositeConfig, Config, ConfigLoader {

}
