package netflix.archaius.typesafe;

import java.io.File;
import java.net.URL;

import netflix.archaius.ConfigLoader;
import netflix.archaius.exceptions.ConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TypesafeConfigLoader implements ConfigLoader {
    public TypesafeConfigLoader() {
    }

    @Override
    public netflix.archaius.Config load(String name, String resourceName) throws ConfigException {
        System.out.println("Loading configuration : " + resourceName);
        Config config = ConfigFactory.parseResources(resourceName + ".properties");
        return new TypesafeConfig(name, config);
    }

    @Override
    public netflix.archaius.Config load(String name, URL url) throws ConfigException {
        return null;
    }

    @Override
    public netflix.archaius.Config load(String name, File file) throws ConfigException {
        return null;
    }

    @Override
    public boolean canLoad(String name) {
        return true;
    }

    @Override
    public boolean canLoad(URL uri) {
        return true;
    }

    @Override
    public boolean canLoad(File file) {
        return true;
    }
}
