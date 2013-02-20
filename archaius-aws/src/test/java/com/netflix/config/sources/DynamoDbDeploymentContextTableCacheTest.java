package com.netflix.config.sources;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.netflix.config.DeploymentContext;
import com.netflix.config.PropertyWithDeploymentContext;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbDeploymentContextTableCacheTest {
    private static final PropertyWithDeploymentContext test1 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "foo", "bar");
    private static final PropertyWithDeploymentContext test2 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "goo", "goo");
    private static final PropertyWithDeploymentContext test3 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "prod", "goo", "foo");
    private static final PropertyWithDeploymentContext test4 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "prod", "goo", "foo");
    private static final PropertyWithDeploymentContext test5 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "boo", "who");

    @Test
    public void testPoll() throws Exception {
        AmazonDynamoDB mockContextDbClient = mock(AmazonDynamoDB.class);

        when(mockContextDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.contextScanResult1,
                DynamoDbMocks.contextScanResult2);
        DynamoDbDeploymentContextTableCache cache = new DynamoDbDeploymentContextTableCache(mockContextDbClient, 100, 100);

        Collection<PropertyWithDeploymentContext> props = cache.getProperties();
        assertEquals(3, props.size());
        assertTrue(props.contains(test1));
        assertTrue(props.contains(test2));
        assertTrue(props.contains(test5));

        Thread.sleep(150);

        props = cache.getProperties();
        assertEquals(4, props.size());
        assertTrue(props.contains(test1));
        assertTrue(props.contains(test3));
        assertTrue(props.contains(test4));
        assertTrue(props.contains(test5));

    }
}
