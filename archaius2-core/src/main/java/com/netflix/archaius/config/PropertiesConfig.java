package com.netflix.archaius.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfig extends MapConfig {

    public static PropertiesConfig fromFile(String filename) throws IOException {
        Properties prop = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (in == null) 
                throw new IOException("Resource " + filename + " not found");
            prop.load(in);
            return new PropertiesConfig(prop);
        }
    }
    
    public PropertiesConfig(Properties props) {
        super(props);
    }

}
