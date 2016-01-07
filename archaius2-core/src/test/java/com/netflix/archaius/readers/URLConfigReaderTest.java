package com.netflix.archaius.readers;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

/**
 * @author Nikos Michalakis <nikos@netflix.com>
 */
public class URLConfigReaderTest {

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
}
