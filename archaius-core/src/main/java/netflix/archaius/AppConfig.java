package netflix.archaius;

import netflix.archaius.config.CompositeConfig;
import netflix.archaius.config.SettableConfig;

public interface AppConfig extends PropertyFactory, SettableConfig, CompositeConfig, ConfigLoader {

}
