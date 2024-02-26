package com.netflix.archaius.readers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Nikos Michalakis <nikos@netflix.com>
 */
public class URLConfigReaderTest {

    @Test
    public void testStringConstructorCommonCase() {
        final String url1 = "http://hello:8080/hey";
        final String url2 = "http://hello:8080/heytoo";
        URLConfigReader reader1 = new URLConfigReader(url1);
        assertEquals(url1, reader1.getConfigUrls().get(0).toString());

        URLConfigReader reader2 = new URLConfigReader(url1, url2);
        assertEquals(url1, reader2.getConfigUrls().get(0).toString());
        assertEquals(url2, reader2.getConfigUrls().get(1).toString());
    }

    @Test
    public void testStringConstructorMalformedUrl() {
        assertThrows(RuntimeException.class, () -> new URLConfigReader("bad url"));
    }
}
