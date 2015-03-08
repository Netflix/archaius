package netflix.archaius.typesafe;

import java.net.URL;

import netflix.archaius.ConfigReader;
import netflix.archaius.exceptions.ConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class TypesafeConfigReader implements ConfigReader {
    @Override
    public netflix.archaius.Config load(ClassLoader loader, String name, String resourceName) throws ConfigException {
        Config config = ConfigFactory.parseResources(loader, resourceName + ".properties");
        return new TypesafeConfig(name, config);
    }

    @Override
    public netflix.archaius.Config load(ClassLoader loader, String name, URL url) throws ConfigException {
        Config config = ConfigFactory.parseURL(url, ConfigParseOptions.defaults().setClassLoader(loader));
        return new TypesafeConfig(name, config);
    }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return true;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return true;
    }
}
