package netflix.archaius.loaders;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import netflix.archaius.Config;
import netflix.archaius.ConfigReader;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigException;
import netflix.archaius.readers.URLConfigReader;

public class PropertiesConfigReader implements ConfigReader {

    @Override
    public Config load(String name, String resourceName) throws ConfigException {
        URL url = getResource(resourceName + ".properties");
        if (url == null) {
            throw new ConfigException("Unable to resolve URL for resource " + resourceName);
        }
        return load(name, url);
    }

    @Override
    public Config load(String name, URL url) throws ConfigException {
        try {
            return new MapConfig(name, new URLConfigReader(url).call());
        } catch (IOException e) {
            throw new ConfigException("Unable to load URL " + url.toString(), e);
        }
    }

    @Override
    public Config load(String name, File file) throws ConfigException {
        try {
            return load(name, (file).toURI().toURL());
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to load file " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public boolean canLoad(String name) {
        return getResource(name + ".properties") != null;
    }

    @Override
    public boolean canLoad(URL uri) {
        return uri.getPath().endsWith(".properties");
    }

    @Override
    public boolean canLoad(File file) {
        return file.getAbsolutePath().endsWith(".properties");
    }
    
    private static URL getResource(String resourceName)
    {
        URL url = null;
        // attempt to load from the context classpath
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
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
