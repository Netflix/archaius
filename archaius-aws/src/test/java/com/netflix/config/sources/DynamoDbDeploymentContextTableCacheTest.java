package com.netflix.config.sources;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.netflix.config.DeploymentContext;
import com.netflix.config.PropertyWithDeploymentContext;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbDeploymentContextTableCacheTest {

    @Test
    public void testPoll() throws Exception {
        AmazonDynamoDB mockContextDbClient = mock(AmazonDynamoDB.class);

        //3 of the first config to cover: object creation, threadRun at 0 delay, load properties
        when(mockContextDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.contextScanResult1);
        DynamoDbDeploymentContextTableCache cache = new DynamoDbDeploymentContextTableCache(mockContextDbClient, 0, 1000);

        Collection<PropertyWithDeploymentContext> props = cache.getProperties();
        assertEquals(0, props.size());

        Thread.sleep(50);

        props = cache.getProperties();
        assertEquals(3, props.size());
        //assertEquals("bar", result.getComplete().get("foo"));
        //assertEquals("goo", result.getComplete().get("goo"));
        //assertEquals("who", result.getComplete().get("boo"));
    }
}
