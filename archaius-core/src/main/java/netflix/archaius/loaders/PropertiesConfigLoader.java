package netflix.archaius.loaders;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import netflix.archaius.Config;
import netflix.archaius.ConfigLoader;
import netflix.archaius.config.MapConfig;
import netflix.archaius.exceptions.ConfigurationException;
import netflix.archaius.readers.URLConfigReader;

public class PropertiesConfigLoader implements ConfigLoader {

    @Override
    public Config load(String name, String resourceName) throws ConfigurationException {
        URL url = getResource(resourceName + ".properties");
        if (url == null) {
            throw new ConfigurationException("Unable to resolve URL for resource " + resourceName);
        }
        return load(name, url);
    }

    @Override
    public Config load(String name, URL url) throws ConfigurationException {
        try {
            return new MapConfig(name, new URLConfigReader(url).call());
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load URL " + url.toString(), e);
        }
    }

    @Override
    public Config load(String name, File file) throws ConfigurationException {
        try {
            return load(name, (file).toURI().toURL());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Unable to load file " + file.getAbsolutePath(), e);
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
