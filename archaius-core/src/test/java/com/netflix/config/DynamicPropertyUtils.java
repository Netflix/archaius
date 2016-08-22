package com.netflix.config;

import java.io.*;
import java.util.Map;

public class DynamicPropertyUtils {

    public static File createConfigFile(Map<String, Object> props) throws IOException {
        File configFile = File.createTempFile("config", "properties");
        writeToFile(props, configFile);
        return configFile;
    }

    public static void updateConfigFile(File configFile, Map<String, Object> props) throws IOException {
        writeToFile(props, configFile);
    }

    private static void writeToFile(Map<String, Object> props, File configFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));

        for (Map.Entry<String, Object> entry : props.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue());
            writer.newLine();
        }

        writer.close();
    }
}
