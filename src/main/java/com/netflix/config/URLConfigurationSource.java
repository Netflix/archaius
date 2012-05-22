/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A polled configuration source based on a set of property files. For each poll,
 * it always returns the complete union of properties defined in all files. If one property
 * is defined in more than one file, the value in file later on the list will override
 * the value in the previous one. The file format is the one of a Java property file.
 * 
 * @author awang
 *
 */
public class URLConfigurationSource implements PolledConfigurationSource {

    private final URL[] configUrls;
    
    public static final String CONFIG_URL = "configurationSource.additionalUrls";
    
    public static final String DEFAULT_CONFIG_FILE_FROM_CLASSPATH = 
        System.getProperty("configurationSource.defaultFileName") == null ? "config.properties" : System.getProperty("configurationSource.defaultFileName");
    
    /**
     * Create FileConfigurationSource with a list of property file names.
     * 
     * @param fileNames a list of property file names.
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
    
    public URLConfigurationSource(URL... url) {
        configUrls = url;
    }
    
    /**
     * Create the FileConfigurationSource for the list of property files specified by 
     * System property <code>configurationSource.fileNames</code>. If there is more than one
     * file, they should be separated by comma. Here is a command line example, 
     * <pre>-DconfigurationSource.fileNames="component1.properties,component2.properties,component3.properties"</pre>
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
                throw new RuntimeException("System property " + CONFIG_URL + " is undefined and default configuration file " 
                        + DEFAULT_CONFIG_FILE_FROM_CLASSPATH + " cannot be found on classpath. At least one of them has to be supplied.");
        } else {
            configUrls = urlList.toArray(new URL[urlList.size()]);
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
     * returns the complete union of properties defined in all files. If one
     * property is defined in more than one file, the value in file later on the
     * list will override the value in the previous one. The file format is the
     * one of a Java property file.
     * 
     * @param initial this parameter is ignored by the implementation
     * @param fullContentRequested this parameter is ignored by the implementation
     * @throws IOException IOException occurred in file operation
     */
    @Override
    public PollResult poll(boolean initial, Object checkPoint)
            throws IOException {        
        Map<String, Object> map = new HashMap<String, Object>();
        for (URL url: configUrls) {
            Properties props = new Properties();
            InputStream fin = url.openStream();
            props.load(fin);
            fin.close();
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
