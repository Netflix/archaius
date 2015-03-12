package com.netflix.archaius.readers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;

public class URLConfigReader implements Callable<Map<String, Object>> {
    private final URL[] configUrls;

    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigReader(String... urls) {
       configUrls = createUrls(urls);
    }
    
    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigReader(URL... urls) {
        configUrls = urls;
    }

    private static URL[] createUrls(String... urlStrings) {
        if (urlStrings == null || urlStrings.length == 0) {
            throw new IllegalArgumentException("urlStrings is null or empty");
        }
        URL[] urls = new URL[urlStrings.length];
        try {
            for (int i = 0; i < urls.length; i++) {
                urls[i] = new URL(urlStrings[i]);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return urls;        
    }
    
    @Override
    public Map<String, Object> call() throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        for (URL url: configUrls) {
            Properties props = new Properties();
            InputStream fin = url.openStream();
            try {
                props.load(fin);
            }
            finally {
                fin.close();
            }
            for (Entry<Object, Object> entry: props.entrySet()) {
                map.put((String) entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    public List<URL> getConfigUrls() {
        return Collections.unmodifiableList(Arrays.asList(configUrls));
    }

    @Override
    public String toString() {
        return "FileConfigurationSource [fileUrls=" + Arrays.toString(configUrls)
                + "]";
    }    
}
