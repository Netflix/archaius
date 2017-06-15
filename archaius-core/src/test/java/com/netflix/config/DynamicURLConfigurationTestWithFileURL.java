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
package com.netflix.config;

import com.netflix.config.DynamicURLConfiguration;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DynamicURLConfigurationTestWithFileURL {

    @BeforeClass
    public static void init() {
        System.setProperty("archaius.configurationSource.defaultFileName", "test.properties");
    }
    
    @Test
    public void testFileURL() {
        DynamicURLConfiguration config = new DynamicURLConfiguration();
        Assert.assertEquals(5, config.getInt("com.netflix.config.samples.SampleApp.SampleBean.numSeeds"));
    }

    
    @Test
    public void testFileURLWithPropertiesUpdatedDynamically() throws IOException, InterruptedException {

        File file = File.createTempFile("DynamicURLConfigurationTestWithFileURL", "testFileURLWithPropertiesUpdatedDynamically");
        populateFile(file, "test.host=12312,123213", "test.host1=13212");

        AbstractConfiguration.setDefaultListDelimiter(',');
        DynamicURLConfiguration config = new DynamicURLConfiguration(0, 500, false, file.toURI().toString());
        Thread.sleep(1000);
        Assert.assertEquals(13212, config.getInt("test.host1"));
        Thread.sleep(1000);
        populateFile(file, "test.host=12312,123213", "test.host1=13212");
        populateFile(file, "test.host=12312,123213", "test.host1=13212");
        CopyOnWriteArrayList writeList = new CopyOnWriteArrayList();
        writeList.add("12312");
        writeList.add("123213");
        config.setProperty("sample.domain", "google,yahoo");
        Assert.assertEquals(writeList, config.getProperty("test.host"));
        

    }
    
    @Test
    public void testChineseCharacters(){
        DynamicURLConfiguration config = new DynamicURLConfiguration();
        Assert.assertEquals("\u4E2D\u6587\u6D4B\u8BD5", config.getString("com.netflix.test-subject"));
    }

    private void populateFile(File temporary, String prop1, String prop2) throws IOException {

        String s = prop1 + "\n" + prop2 + "\n";
        byte data[] = s.getBytes("UTF-8");
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(temporary, true), 8 * 1024);
            out.write(data, 0, data.length);
        } finally {
            if (null != out) {
                out.flush();
                out.close();
            }
        }

    }

}
