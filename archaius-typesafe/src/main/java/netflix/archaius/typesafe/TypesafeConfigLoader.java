package netflix.archaius.typesafe;

import java.io.File;
import java.net.URL;

import netflix.archaius.ConfigLoader;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TypesafeConfigLoader implements ConfigLoader {
    public TypesafeConfigLoader() {
    }

    @Override
    public netflix.archaius.Config load(String name) {
        System.out.println("Loading configuration : " + name);
        Config config = ConfigFactory.parseResources(name + ".properties");
        return new TypesafeConfig(name, config);
    }

    @Override
    public netflix.archaius.Config load(URL name) {
        return null;
    }

    @Override
    public netflix.archaius.Config load(File file) {
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
