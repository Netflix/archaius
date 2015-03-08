package netflix.archaius.loaders;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import netflix.archaius.Config;
import netflix.archaius.ConfigReader;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.readers.URLConfigReader;

public class PropertiesConfigReader implements ConfigReader {

    @Override
    public Config load(ClassLoader loader, String name, String resourceName) throws ConfigException {
        URL url = getResource(loader, resourceName + ".properties");
        if (url == null) {
            throw new ConfigException("Unable to resolve URL for resource " + resourceName);
        }
        return load(loader, name, url);
    }

    @Override
    public Config load(ClassLoader loader, String name, URL url) throws ConfigException {
        try {
            return new MapConfig(name, new URLConfigReader(url).call());
        } catch (IOException e) {
            throw new ConfigException("Unable to load URL " + url.toString(), e);
        }
    }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return getResource(loader, name + ".properties") != null;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return uri.getPath().endsWith(".properties");
    }

    private static URL getResource(ClassLoader loader, String resourceName)
    {
        URL url = null;
        // attempt to load from the context classpath
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        
        if (loader != null) {
            url = loader.getResource(resourceName);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = ClassLoader.getSystemResource(resourceName);
        }
        if (url == null) {
            try {
                resourceName = URLDecoder.decode(resourceName, "UTF-8");
                url = (new File(resourceName)).toURI().toURL();
            } catch (Exception e) {

            }
        }
        return url;
    }
}
