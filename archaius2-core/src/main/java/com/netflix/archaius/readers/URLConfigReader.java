/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.netflix.archaius.config.polling.PollingResponse;

public class URLConfigReader implements Callable<PollingResponse> {
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
        configUrls = Arrays.copyOf(urls, urls.length);
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
    public PollingResponse call() throws IOException {
        final Map<String, String> map = new HashMap<>();
        Properties props = new Properties();
        for (URL url: configUrls) {
            try (InputStream fin = url.openStream()) {
                try (Reader reader = new InputStreamReader(fin, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }

            for (Entry<Object, Object> entry: props.entrySet()) {
                map.put((String) entry.getKey(), entry.getValue().toString());
            }

            props.clear();
        }

        return new PollingResponse() {
            @Override
            public Map<String, String> getToAdd() {
                return map;
            }

            @Override
            public Collection<String> getToRemove() {
                return Collections.emptyList();
            }

            @Override
            public boolean hasData() {
                return true;
            }
        };
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
