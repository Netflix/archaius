package com.netflix.config.sources;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.PollResult;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
// temporarily disabled due to possible AWS key access issue 
@Ignore
public class DynamoDbConfigurationSourceTest {

    @Test
    public void testPoll() throws Exception {
        AmazonDynamoDB mockBasicDbClient = mock(AmazonDynamoDB.class);
        when(mockBasicDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.basicScanResult1);

        DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource(mockBasicDbClient);
        PollResult result = testConfigSource.poll(false, null);
        assertEquals(3, result.getComplete().size());
        assertEquals("bar", result.getComplete().get("foo"));
        assertEquals("goo", result.getComplete().get("goo"));
        assertEquals("who", result.getComplete().get("boo"));
    }

    @Test
    public void testUpdate() throws Exception {
        AmazonDynamoDB mockBasicDbClient = mock(AmazonDynamoDB.class);
        when(mockBasicDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.basicScanResult1, DynamoDbMocks.basicScanResult2);

        DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource(mockBasicDbClient);

        PollResult result = testConfigSource.poll(false, null);
        assertEquals(3, result.getComplete().size());
        assertEquals("bar", result.getComplete().get("foo"));
        assertEquals("goo", result.getComplete().get("goo"));
        assertEquals("who", result.getComplete().get("boo"));

        result = testConfigSource.poll(false, null);
        assertEquals(3, result.getComplete().size());
        assertEquals("bar", result.getComplete().get("foo"));
        assertEquals("foo", result.getComplete().get("goo"));
        assertEquals("who", result.getComplete().get("boo"));
    }
}
