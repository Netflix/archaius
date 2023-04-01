package com.netflix.archaius.readers;

import com.netflix.archaius.config.polling.PollingResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * @author Nikos Michalakis <nikos@netflix.com>
 */
public class URLConfigReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testStringConstructorCommonCase() {
        final String url1 = "http://hello:8080/hey";
        final String url2 = "http://hello:8080/heytoo";
        URLConfigReader reader1 = new URLConfigReader(url1);
        Assert.assertEquals(url1, reader1.getConfigUrls().get(0).toString());

        URLConfigReader reader2 = new URLConfigReader(url1, url2);
        Assert.assertEquals(url1, reader2.getConfigUrls().get(0).toString());
        Assert.assertEquals(url2, reader2.getConfigUrls().get(1).toString());
    }

    @Test(expected = RuntimeException.class)
    public void testStringConstructorMalformedUrl() {
        new URLConfigReader("bad url");
    }

    @Test
    public void testCall() throws IOException {
        File configFile = folder.newFile();
        Files.write(configFile.toPath(), "prop1=value1\nprop2=value2".getBytes(StandardCharsets.UTF_8));

        File anotherConfigFile = folder.newFile();
        Files.write(anotherConfigFile.toPath(), "prop3=value3\nprop4=value4".getBytes(StandardCharsets.UTF_8));

        URLConfigReader configReader = new URLConfigReader(configFile.toURI().toURL(), anotherConfigFile.toURI().toURL());

        PollingResponse response = configReader.call();
        Assert.assertTrue(response.getToRemove().isEmpty());
        Assert.assertTrue(response.hasData());
        Map<String, String> config = response.getToAdd();

        Assert.assertEquals(4, config.size());
        Assert.assertEquals("value1", config.get("prop1"));
        Assert.assertEquals("value2", config.get("prop2"));
        Assert.assertEquals("value3", config.get("prop3"));
        Assert.assertEquals("value4", config.get("prop4"));
    }
}
