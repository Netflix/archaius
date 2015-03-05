package netflix.archaius.config;

import java.util.Properties;

public interface SettableConfig {
    void setProperties(Properties properties);
    void setProperty(String propName, Object propValue);
    void clearProperty(String propName);
}
