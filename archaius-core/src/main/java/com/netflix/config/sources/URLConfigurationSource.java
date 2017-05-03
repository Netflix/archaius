/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config.sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.util.ConfigurationUtils;

/**
 * A polled configuration source based on a set of URLs. For each poll,
 * it always returns the complete union of properties defined in all files. If one property
 * is defined in more than one URL, the value in file later on the list will override
 * the value in the previous one. The content of the URL should conform to the properties file format.
 * 
 * @author awang
 *
 */
public class URLConfigurationSource implements PolledConfigurationSource {

    private final URL[] configUrls;
    
    /**
     * System property name to define a set of URLs to be used by the
     * default constructor. 
     */
    public static final String CONFIG_URL = "archaius.configurationSource.additionalUrls";
    
    /**
     * Default configuration file name to be used by default constructor. This file should
     * be on the classpath. The file name can be overridden by the value of system property
     * {@link #DEFAULT_CONFIG_FILE_PROPERTY}.
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.properties";

    /**
     * System property name to be used to learn the location of the configuration file name
     * on the classpath.
     */
    public static final String DEFAULT_CONFIG_FILE_PROPERTY = "archaius.configurationSource.defaultFileName";

    public static final String DEFAULT_CONFIG_FILE_FROM_CLASSPATH = System.getProperty(DEFAULT_CONFIG_FILE_PROPERTY, DEFAULT_CONFIG_FILE_NAME);

    private static final Logger logger = LoggerFactory.getLogger(URLConfigurationSource.class);

    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigurationSource(String... urls) {
       configUrls = createUrls(urls);
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
    
    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigurationSource(URL... urls) {
        configUrls = urls;
    }
    
    /**
     * Create the instance for the default list of URLs, which is composed by the following order
     * 
     * <ul>
     * <li>A configuration file (default name to be <code>config.properties</code>, see {@link #DEFAULT_CONFIG_FILE_NAME}) on the classpath
     * <li>A list of URLs defined by system property {@value #CONFIG_URL} with values separated by comma <code>","</code>.
     * </ul>
     */
    public URLConfigurationSource() {
        List<URL> urlList = new ArrayList<URL>();
        URL configFromClasspath = getConfigFileFromClasspath();
        if (configFromClasspath != null) {
            urlList.add(configFromClasspath);
        }
        String[] fileNames = getDefaultFileSources();
        if (fileNames.length != 0) {
            urlList.addAll(Arrays.asList(createUrls(fileNames)));                    
        } 
        if (urlList.size() == 0) { 
            configUrls = new URL[0];
            logger.warn("No URLs will be polled as dynamic configuration sources.");
            logger.info("To enable URLs as dynamic configuration sources, define System property " 
                    + CONFIG_URL + " or make " + DEFAULT_CONFIG_FILE_FROM_CLASSPATH + " available on classpath.");
        } else {
            configUrls = urlList.toArray(new URL[urlList.size()]);
            logger.info("URLs to be used as dynamic configuration source: " + urlList);
        }
    }
    
    private URL getConfigFileFromClasspath() {
        URL url = null;
        // attempt to load from the context classpath
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            url = loader.getResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = ClassLoader.getSystemResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = URLConfigurationSource.class.getResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        return url;
    }
    
    public List<URL> getConfigUrls() {
        return Collections.unmodifiableList(Arrays.asList(configUrls));
    }
    
    private static final String[] getDefaultFileSources() {
        String name = System.getProperty(CONFIG_URL);
        String[] fileNames;
        if (name != null) {
            fileNames = name.split(",");
        } else {
            fileNames = new String[0];
        }
        return fileNames;
    }
    
    
    /**
     * Retrieve the content of the property files. For each poll, it always
     * returns the complete union of properties defined in all URLs. If one
     * property is defined in content of more than one URL, the value in file later on the
     * list will override the value in the previous one. 
     * 
     * @param initial this parameter is ignored by the implementation
     * @param checkPoint this parameter is ignored by the implementation
     * @throws IOException IOException occurred in file operation
     */
    @Override
    public PollResult poll(boolean initial, Object checkPoint)
            throws IOException {    
        if (configUrls == null || configUrls.length == 0) {
            return PollResult.createFull(null);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (URL url: configUrls) {
            InputStream fin = url.openStream();
            Properties props = ConfigurationUtils.loadPropertiesFromInputStream(fin);
            for (Entry<Object, Object> entry: props.entrySet()) {
                map.put((String) entry.getKey(), entry.getValue());
            }
        }
        return PollResult.createFull(map);
    }

    @Override
    public String toString() {
        return "FileConfigurationSource [fileUrls=" + Arrays.toString(configUrls)
                + "]";
    }    
}
