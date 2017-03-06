package com.netflix.config.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.netflix.config.PollResult;

/**
 * Description.
 * @author Your Name
 */
public class URLConfigurationSourceTest {

    
    @Test
    public void testMultilineKey() throws IOException {
        URL multilineProperties = Thread.currentThread().getContextClassLoader().getResource("multiline.properties");
        
        URLConfigurationSource configSource = new URLConfigurationSource(multilineProperties);
        
        PollResult pollResult = configSource.poll(true, null);
        
        assertNotNull(pollResult);
        Object multipleLine = pollResult.getComplete().get("multiline.key");
        Object multipleValue = pollResult.getComplete().get("multivalue.key");
        
        assertEquals(multipleLine, multipleValue);
    }
}
